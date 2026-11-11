//! Quick-add natural language parser.
//!
//! `"내일 15:00 백엔드 API 리팩토링 #개발 !1 ~2h"` →
//! date = tomorrow, time = 15:00, title = "백엔드 API 리팩토링", tags = [개발],
//! priority = high, estimate = 120 min.
//!
//! Tokens (anywhere in the line):
//! - Date: 오늘 / 내일 / 모레 / 글피, `N일 후`, `월요일`…`일요일` (next one),
//!   `이번주|다음주 금요일`, `M/D`, `M월 D일`, `YYYY-MM-DD`, today / tomorrow
//! - Time: `15:00`, `15시`, `15시 30분`, `오후 3시`, `오전 9시 30분`, `3pm`, `9:30am`
//! - `#태그`, `!1` (높음) `!2` (중간) `!3` (낮음)
//! - `~2h`, `~30m`, `~1h30m`, `~45` (minutes)
//! - Energy: `@고집중`/`@focus`, `@단순`/`@routine`, `@체력`/`@physical`

use chrono::{Datelike, Duration, NaiveDate, Weekday};
use serde::Serialize;

use crate::db::{EnergyLevel, NewTask};

#[derive(Debug, Clone, Default, PartialEq, Serialize)]
pub struct ParsedQuickAdd {
    pub title: String,
    pub due_date: Option<NaiveDate>,
    pub due_time: Option<String>,
    pub tags: Vec<String>,
    /// 0 none … 3 high.
    pub priority: u8,
    pub estimated_minutes: Option<u32>,
    pub energy_level: Option<EnergyLevel>,
}

impl ParsedQuickAdd {
    pub fn into_new_task(self) -> NewTask {
        NewTask {
            title: self.title,
            priority: self.priority,
            due_date: self.due_date,
            due_time: self.due_time,
            energy_level: self.energy_level,
            estimated_minutes: self.estimated_minutes,
            tags: self.tags,
            source: Some("quickadd".into()),
            ..Default::default()
        }
    }
}

pub fn parse(input: &str, today: NaiveDate) -> ParsedQuickAdd {
    let tokens: Vec<&str> = input.split_whitespace().collect();
    let mut out = ParsedQuickAdd::default();
    let mut title: Vec<&str> = Vec::new();
    // Set by a standalone 오전 / 오후 that precedes an hour token.
    let mut meridiem: Option<bool> = None; // Some(true) = PM
    let mut i = 0;

    while i < tokens.len() {
        let tok = tokens[i];
        let next = tokens.get(i + 1).copied();

        // #tag
        if let Some(tag) = tok.strip_prefix('#').filter(|t| !t.is_empty()) {
            if !out.tags.iter().any(|t| t == tag) {
                out.tags.push(tag.to_string());
            }
            i += 1;
            continue;
        }
        // !1..!3
        if let Some(p) = tok.strip_prefix('!').and_then(|p| p.parse::<u8>().ok()) {
            if (1..=3).contains(&p) {
                out.priority = 4 - p;
                i += 1;
                continue;
            }
        }
        // ~estimate
        if let Some(est) = tok.strip_prefix('~').and_then(parse_duration_minutes) {
            out.estimated_minutes = Some(est);
            i += 1;
            continue;
        }
        // @energy
        if let Some(e) = tok.strip_prefix('@').and_then(parse_energy) {
            out.energy_level = Some(e);
            i += 1;
            continue;
        }

        if out.due_date.is_none() {
            if let Some((date, used)) = parse_date(tok, next, today) {
                out.due_date = Some(date);
                i += used;
                continue;
            }
        }

        if out.due_time.is_none() {
            if tok == "오전" || tok == "오후" {
                if next.is_some_and(|n| parse_time(n, None, tokens.get(i + 2).copied()).is_some()) {
                    meridiem = Some(tok == "오후");
                    i += 1;
                    continue;
                }
            }
            if let Some((time, used)) = parse_time(tok, meridiem, next) {
                out.due_time = Some(time);
                i += used;
                continue;
            }
        }

        title.push(tok);
        i += 1;
    }

    // A bare time with no date means today.
    if out.due_time.is_some() && out.due_date.is_none() {
        out.due_date = Some(today);
    }
    out.title = title.join(" ");
    out
}

fn parse_energy(s: &str) -> Option<EnergyLevel> {
    match s.to_lowercase().as_str() {
        "고집중" | "집중" | "focus" | "deep" | "high" => Some(EnergyLevel::HighFocus),
        "단순" | "루틴" | "routine" | "low" => Some(EnergyLevel::LowRoutine),
        "체력" | "운동" | "physical" | "body" => Some(EnergyLevel::Physical),
        _ => None,
    }
}

/// `2h`, `30m`, `1h30m`, `1.5h`, `45` (minutes), `2시간`, `30분`.
fn parse_duration_minutes(s: &str) -> Option<u32> {
    let s = s.to_lowercase().replace("시간", "h").replace('분', "m");
    if let Ok(m) = s.parse::<u32>() {
        return (m > 0).then_some(m);
    }
    let mut total = 0f64;
    let mut num = String::new();
    let mut any = false;
    for ch in s.chars() {
        match ch {
            '0'..='9' | '.' => num.push(ch),
            'h' | 'm' => {
                let v: f64 = num.parse().ok()?;
                total += if ch == 'h' { v * 60.0 } else { v };
                num.clear();
                any = true;
            }
            _ => return None,
        }
    }
    if !num.is_empty() || !any || total <= 0.0 {
        return None;
    }
    Some(total.round() as u32)
}

fn weekday_from_ko(s: &str) -> Option<Weekday> {
    let base = s.strip_suffix("요일")?;
    Some(match base {
        "월" => Weekday::Mon,
        "화" => Weekday::Tue,
        "수" => Weekday::Wed,
        "목" => Weekday::Thu,
        "금" => Weekday::Fri,
        "토" => Weekday::Sat,
        "일" => Weekday::Sun,
        _ => return None,
    })
}

/// Returns the date and how many tokens it consumed.
fn parse_date(tok: &str, next: Option<&str>, today: NaiveDate) -> Option<(NaiveDate, usize)> {
    let rel = |d: i64| Some((today + Duration::days(d), 1));
    match tok.to_lowercase().as_str() {
        "오늘" | "today" => return rel(0),
        "내일" | "tomorrow" => return rel(1),
        "모레" => return rel(2),
        "글피" => return rel(3),
        _ => {}
    }

    // N일 후 / N일뒤 / N일후
    if let Some(n) = tok.strip_suffix("일후").or_else(|| tok.strip_suffix("일뒤")) {
        if let Ok(n) = n.parse::<i64>() {
            return Some((today + Duration::days(n), 1));
        }
    }
    if let (Some(n), Some("후" | "뒤")) = (tok.strip_suffix('일'), next) {
        if let Ok(n) = n.parse::<i64>() {
            return Some((today + Duration::days(n), 2));
        }
    }

    // 이번주 / 다음주 + 요일
    if matches!(tok, "이번주" | "다음주" | "담주") {
        if let Some(wd) = next.and_then(weekday_from_ko) {
            let monday = today - Duration::days(today.weekday().num_days_from_monday() as i64);
            let week = if tok == "이번주" { 0 } else { 7 };
            return Some((monday + Duration::days(week + wd.num_days_from_monday() as i64), 2));
        }
    }
    // 요일 alone → next occurrence (never today).
    if let Some(wd) = weekday_from_ko(tok) {
        let mut ahead = (wd.num_days_from_monday() as i64 - today.weekday().num_days_from_monday() as i64)
            .rem_euclid(7);
        if ahead == 0 {
            ahead = 7;
        }
        return Some((today + Duration::days(ahead), 1));
    }

    // YYYY-MM-DD
    if let Ok(d) = NaiveDate::parse_from_str(tok, "%Y-%m-%d") {
        return Some((d, 1));
    }
    // M/D → this year, or next year if already past.
    if let Some((m, d)) = tok.split_once('/') {
        if let (Ok(m), Ok(d)) = (m.parse::<u32>(), d.parse::<u32>()) {
            return upcoming_month_day(today, m, d).map(|x| (x, 1));
        }
    }
    // M월 D일 / M월D일
    if let Some(m) = tok.strip_suffix('월').and_then(|m| m.parse::<u32>().ok()) {
        if let Some(d) = next.and_then(|n| n.strip_suffix('일')).and_then(|d| d.parse::<u32>().ok()) {
            return upcoming_month_day(today, m, d).map(|x| (x, 2));
        }
    }
    if let Some((m, rest)) = tok.split_once('월') {
        if let (Ok(m), Some(Ok(d))) = (m.parse::<u32>(), rest.strip_suffix('일').map(str::parse::<u32>)) {
            return upcoming_month_day(today, m, d).map(|x| (x, 1));
        }
    }
    None
}

fn upcoming_month_day(today: NaiveDate, m: u32, d: u32) -> Option<NaiveDate> {
    let this_year = NaiveDate::from_ymd_opt(today.year(), m, d)?;
    if this_year >= today {
        Some(this_year)
    } else {
        NaiveDate::from_ymd_opt(today.year() + 1, m, d)
    }
}

/// `15:00`, `15시`, `15시30분`, `15시 30분`, `3pm`, `9:30am`. Returns "HH:MM" and tokens used.
fn parse_time(tok: &str, pm: Option<bool>, next: Option<&str>) -> Option<(String, usize)> {
    let lower = tok.to_lowercase();
    let (body, suffix_pm) = if let Some(b) = lower.strip_suffix("pm") {
        (b.to_string(), Some(true))
    } else if let Some(b) = lower.strip_suffix("am") {
        (b.to_string(), Some(false))
    } else {
        (lower.clone(), None)
    };
    let pm = suffix_pm.or(pm);

    let (mut h, m, used): (u32, u32, usize) = if let Some((h, m)) = body.split_once(':') {
        (h.parse().ok()?, m.parse().ok()?, 1)
    } else if let Some((h, rest)) = body.split_once('시') {
        let h: u32 = h.parse().ok()?;
        if rest.is_empty() {
            match next.and_then(|n| n.strip_suffix('분')).and_then(|m| m.parse::<u32>().ok()) {
                Some(m) => (h, m, 2),
                None => (h, 0, 1),
            }
        } else if rest == "반" {
            (h, 30, 1)
        } else {
            (h, rest.strip_suffix('분')?.parse().ok()?, 1)
        }
    } else if suffix_pm.is_some() {
        (body.parse().ok()?, 0, 1)
    } else {
        return None;
    };

    match pm {
        Some(true) if h < 12 => h += 12,
        Some(false) if h == 12 => h = 0,
        _ => {}
    }
    (h < 24 && m < 60).then(|| (format!("{h:02}:{m:02}"), used))
}

#[cfg(test)]
mod tests {
    use super::*;

    // Wednesday.
    fn today() -> NaiveDate {
        NaiveDate::from_ymd_opt(2026, 11, 11).unwrap()
    }
    fn d(y: i32, m: u32, dd: u32) -> Option<NaiveDate> {
        NaiveDate::from_ymd_opt(y, m, dd)
    }

    #[test]
    fn spec_example() {
        let p = parse("내일 15:00 백엔드 API 리팩토링 #개발 !1 ~2h", today());
        assert_eq!(
            p,
            ParsedQuickAdd {
                title: "백엔드 API 리팩토링".into(),
                due_date: d(2026, 11, 12),
                due_time: Some("15:00".into()),
                tags: vec!["개발".into()],
                priority: 3,
                estimated_minutes: Some(120),
                energy_level: None,
            }
        );
    }

    #[test]
    fn korean_times_and_energy() {
        let p = parse("오후 3시 30분 헬스 @체력 ~1h30m", today());
        assert_eq!(p.title, "헬스");
        assert_eq!(p.due_date, d(2026, 11, 11));
        assert_eq!(p.due_time.as_deref(), Some("15:30"));
        assert_eq!(p.energy_level, Some(EnergyLevel::Physical));
        assert_eq!(p.estimated_minutes, Some(90));

        assert_eq!(parse("오전 12시 알람", today()).due_time.as_deref(), Some("00:00"));
        assert_eq!(parse("9:30am 회의", today()).due_time.as_deref(), Some("09:30"));
        assert_eq!(parse("모레 8시반 출근", today()).due_time.as_deref(), Some("08:30"));
    }

    #[test]
    fn dates() {
        assert_eq!(parse("금요일 보고서", today()).due_date, d(2026, 11, 13));
        assert_eq!(parse("수요일 보고서", today()).due_date, d(2026, 11, 18));
        assert_eq!(parse("다음주 월요일 보고서", today()).due_date, d(2026, 11, 16));
        assert_eq!(parse("이번주 일요일 정리", today()).due_date, d(2026, 11, 15));
        assert_eq!(parse("3일 후 병원", today()).due_date, d(2026, 11, 14));
        assert_eq!(parse("12/25 크리스마스", today()).due_date, d(2026, 12, 25));
        assert_eq!(parse("1/2 새해", today()).due_date, d(2027, 1, 2));
        assert_eq!(parse("3월 1일 신청", today()).due_date, d(2027, 3, 1));
        assert_eq!(parse("2026-12-01 마감", today()).due_date, d(2026, 12, 1));
    }

    #[test]
    fn plain_text_stays_title() {
        let p = parse("월요병 극복하기 !9 ~abc", today());
        assert_eq!(p.title, "월요병 극복하기 !9 ~abc");
        assert_eq!(p.due_date, None);
        assert_eq!(p.priority, 0);
    }
}

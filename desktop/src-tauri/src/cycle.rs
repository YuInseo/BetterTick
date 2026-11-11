//! Custom N-day cycle engine (Feature B).
//!
//! A pattern is an anchor date (slot 0) plus N labels. The state of any date is
//! `(date - anchor).rem_euclid(N)` — O(1), and correct for dates before the
//! anchor too. Per-day overrides (근무 바꾸기) win over the rotation.

use std::collections::HashMap;

use chrono::NaiveDate;

pub struct CyclePattern {
    anchor: NaiveDate,
    slots: Vec<(String, Option<String>)>,
    overrides: HashMap<NaiveDate, String>,
}

impl CyclePattern {
    pub fn new(
        anchor: NaiveDate,
        slots: Vec<(String, Option<String>)>,
        overrides: HashMap<NaiveDate, String>,
    ) -> Self {
        assert!(!slots.is_empty(), "cycle needs at least one slot");
        Self { anchor, slots, overrides }
    }

    /// Slot index of `date` in the rotation — O(1).
    pub fn slot_index(&self, date: NaiveDate) -> usize {
        slot_index(self.anchor, self.slots.len(), date)
    }

    /// Label on `date` and whether it came from an override.
    pub fn label_on(&self, date: NaiveDate) -> (&str, bool) {
        match self.overrides.get(&date) {
            Some(l) => (l.as_str(), true),
            None => (self.slots[self.slot_index(date)].0.as_str(), false),
        }
    }

    pub fn color_of(&self, label: &str) -> Option<String> {
        self.slots.iter().find(|(l, _)| l == label).and_then(|(_, c)| c.clone())
    }
}

/// `(date - anchor) mod len`, always in `0..len`.
pub fn slot_index(anchor: NaiveDate, len: usize, date: NaiveDate) -> usize {
    let days = date.signed_duration_since(anchor).num_days();
    days.rem_euclid(len as i64) as usize
}

#[cfg(test)]
mod tests {
    use super::*;

    fn d(s: &str) -> NaiveDate {
        NaiveDate::parse_from_str(s, "%Y-%m-%d").unwrap()
    }

    #[test]
    fn wraps_forward_and_backward() {
        let a = d("2026-11-01");
        assert_eq!(slot_index(a, 4, a), 0);
        assert_eq!(slot_index(a, 4, d("2026-11-04")), 3);
        assert_eq!(slot_index(a, 4, d("2026-11-05")), 0);
        assert_eq!(slot_index(a, 4, d("2026-10-31")), 3);
        assert_eq!(slot_index(a, 3, d("2025-11-01")), (-365i64).rem_euclid(3) as usize);
    }

    #[test]
    fn overrides_win() {
        let labels = ["주간", "야간", "비번", "휴무"].map(|l| (l.to_string(), None)).to_vec();
        let mut ov = HashMap::new();
        ov.insert(d("2026-11-02"), "휴무".to_string());
        let p = CyclePattern::new(d("2026-11-01"), labels, ov);
        assert_eq!(p.label_on(d("2026-11-01")), ("주간", false));
        assert_eq!(p.label_on(d("2026-11-02")), ("휴무", true));
        assert_eq!(p.label_on(d("2026-11-06")), ("야간", false));
    }
}

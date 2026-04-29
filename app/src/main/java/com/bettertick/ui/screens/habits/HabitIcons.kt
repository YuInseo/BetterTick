package com.bettertick.ui.screens.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Create
import androidx.compose.material.icons.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Pool
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.SentimentVerySatisfied
import androidx.compose.material.icons.rounded.SmokeFree
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text

data class HabitIconDef(val key: String, val icon: ImageVector, val defaultColor: String)

val habitIconOptions: List<HabitIconDef> = listOf(
    // Row 1 – daily/food
    HabitIconDef("checkin",   Icons.Rounded.CheckCircle,         "#4CAF50"),
    HabitIconDef("water",     Icons.Rounded.LocalDrink,          "#2196F3"),
    HabitIconDef("food",      Icons.Rounded.Restaurant,          "#FF9800"),
    HabitIconDef("cafe",      Icons.Rounded.LocalCafe,           "#795548"),
    HabitIconDef("cake",      Icons.Rounded.Cake,                "#E91E63"),
    HabitIconDef("fastfood",  Icons.Rounded.Fastfood,            "#FF5722"),
    HabitIconDef("sun",       Icons.Rounded.WbSunny,             "#FFC107"),
    // Row 2 – exercise
    HabitIconDef("sleep",     Icons.Rounded.NightsStay,          "#3F51B5"),
    HabitIconDef("walk",      Icons.Rounded.DirectionsWalk,      "#66BB6A"),
    HabitIconDef("run",       Icons.Rounded.DirectionsRun,       "#FF9800"),
    HabitIconDef("gym",       Icons.Rounded.FitnessCenter,       "#E53935"),
    HabitIconDef("bike",      Icons.Rounded.DirectionsBike,      "#2196F3"),
    HabitIconDef("swim",      Icons.Rounded.Pool,                "#03A9F4"),
    HabitIconDef("yoga",      Icons.Rounded.SelfImprovement,     "#9C27B0"),
    // Row 3 – learning
    HabitIconDef("book",      Icons.Rounded.MenuBook,            "#1976D2"),
    HabitIconDef("write",     Icons.Rounded.Create,              "#FF7043"),
    HabitIconDef("library",   Icons.Rounded.LibraryBooks,        "#FF8F00"),
    HabitIconDef("school",    Icons.Rounded.School,              "#7B1FA2"),
    HabitIconDef("language",  Icons.Rounded.Language,            "#00BCD4"),
    HabitIconDef("money",     Icons.Rounded.AttachMoney,         "#4CAF50"),
    HabitIconDef("work",      Icons.Rounded.Work,                "#607D8B"),
    // Row 4 – motivation
    HabitIconDef("task",      Icons.Rounded.Assignment,          "#455A64"),
    HabitIconDef("star",      Icons.Rounded.Star,                "#FFC107"),
    HabitIconDef("heart",     Icons.Rounded.Favorite,            "#E53935"),
    HabitIconDef("like",      Icons.Rounded.ThumbUp,             "#FF7043"),
    HabitIconDef("music",     Icons.Rounded.MusicNote,           "#7B1FA2"),
    HabitIconDef("art",       Icons.Rounded.Palette,             "#9C27B0"),
    HabitIconDef("photo",     Icons.Rounded.PhotoCamera,         "#FF9800"),
    // Row 5 – tech/lifestyle
    HabitIconDef("tv",        Icons.Rounded.Tv,                  "#F44336"),
    HabitIconDef("laptop",    Icons.Rounded.Laptop,              "#37474F"),
    HabitIconDef("puzzle",    Icons.Rounded.Extension,           "#1565C0"),
    HabitIconDef("mind",      Icons.Rounded.Psychology,          "#880E4F"),
    HabitIconDef("pet",       Icons.Rounded.Pets,                "#795548"),
    HabitIconDef("flower",    Icons.Rounded.LocalFlorist,        "#E91E63"),
    HabitIconDef("time",      Icons.Rounded.AccessTime,          "#2196F3"),
    // Row 6 – health/other
    HabitIconDef("target",    Icons.Rounded.GpsFixed,            "#E65100"),
    HabitIconDef("eye",       Icons.Rounded.Visibility,          "#00BCD4"),
    HabitIconDef("phone",     Icons.Rounded.Phone,               "#43A047"),
    HabitIconDef("home",      Icons.Rounded.Home,                "#FF7043"),
    HabitIconDef("camera",    Icons.Rounded.CameraAlt,           "#FF8A65"),
    HabitIconDef("smile",     Icons.Rounded.SentimentVerySatisfied, "#FFC107"),
    HabitIconDef("spa",       Icons.Rounded.Spa,                 "#EC407A"),
    // Row 7 – wellness/nature
    HabitIconDef("nature",    Icons.Rounded.Park,                "#388E3C"),
    HabitIconDef("eco",       Icons.Rounded.Eco,                 "#2E7D32"),
    HabitIconDef("cloud",     Icons.Rounded.Cloud,               "#5C6BC0"),
    HabitIconDef("gift",      Icons.Rounded.CardGiftcard,        "#E91E63"),
    HabitIconDef("coin",      Icons.Rounded.MonetizationOn,      "#FFC107"),
    HabitIconDef("brush",     Icons.Rounded.Brush,               "#9C27B0"),
    HabitIconDef("nosmoke",   Icons.Rounded.SmokeFree,           "#607D8B"),
)

val habitIconMap: Map<String, ImageVector> = habitIconOptions.associate { it.key to it.icon }

@Composable
fun HabitIconView(
    iconKey: String,
    colorHex: String,
    circleSize: Dp = 44.dp,
    iconSize: Dp = 24.dp,
    fallbackText: String = ""
) {
    val bgColor = runCatching { Color(android.graphics.Color.parseColor(colorHex)) }
        .getOrDefault(Color(0xFF9B59B6))
    val vector = habitIconMap[iconKey]

    Box(
        modifier = Modifier
            .size(circleSize)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        if (vector != null) {
            Icon(vector, contentDescription = null, tint = Color.White, modifier = Modifier.size(iconSize))
        } else {
            val displayText = iconKey.ifBlank { fallbackText.take(1) }
            Text(
                text = displayText,
                fontSize = (iconSize.value * 0.65f).sp,
                color = Color.White
            )
        }
    }
}

package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DailyDistance
import com.example.ui.theme.CalmingTeal
import com.example.ui.theme.MedicalBluePrimary

@Composable
fun DistanceChart(
  dailyStats: List<DailyDistance>,
  todayKm: Float,
  weekKm: Float,
  totalKm: Float,
  modifier: Modifier = Modifier
) {
  var selectedDay by remember { mutableStateOf<DailyDistance?>(dailyStats.getOrNull(5)) }
  val maxDistance = (dailyStats.maxOfOrNull { it.distanceKm } ?: 4.0f).coerceAtLeast(3.5f)

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("distance_chart_card"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    border = androidx.compose.foundation.BorderStroke(
      width = 1.dp,
      color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    )
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Activity & Distance Tracker",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Past 7 days mobility log",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "%.2f km".format(todayKm),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = CalmingTeal
          )
          Text(
            text = "Today's Travel",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 3 Overview Metrics Pills
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        MetricTile(title = "Today", value = "%.1f km".format(todayKm), modifier = Modifier.weight(1f))
        MetricTile(title = "This Week", value = "%.1f km".format(weekKm), modifier = Modifier.weight(1f))
        MetricTile(title = "Total Lifetime", value = "%.1f km".format(totalKm), modifier = Modifier.weight(1f))
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Bar Chart Container
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(130.dp)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .align(Alignment.BottomCenter),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.Bottom
        ) {
          dailyStats.forEach { stat ->
            val isToday = stat.day == "Sat" // highlighted day
            val isSelected = selectedDay?.day == stat.day
            val fraction = (stat.distanceKm / maxDistance).coerceIn(0.08f, 1.0f)
            val animatedHeight by animateFloatAsState(
              targetValue = fraction,
              label = "barHeight_${stat.day}"
            )

            val barColor = when {
              isSelected -> CalmingTeal
              isToday -> MedicalBluePrimary
              else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            }

            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier
                .clickable { selectedDay = stat }
                .padding(horizontal = 4.dp)
            ) {
              if (isSelected) {
                Text(
                  text = "%.1f".format(stat.distanceKm),
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = CalmingTeal
                )
                Spacer(modifier = Modifier.height(2.dp))
              }

              Box(
                modifier = Modifier
                  .width(26.dp)
                  .height((90 * animatedHeight).dp)
                  .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                  .background(barColor)
              )

              Spacer(modifier = Modifier.height(6.dp))

              Text(
                text = stat.day,
                fontSize = 11.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isToday) MedicalBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MetricTile(
  title: String,
  value: String,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
      .padding(vertical = 8.dp, horizontal = 10.dp),
    horizontalAlignment = Alignment.Start
  ) {
    Text(
      text = title,
      fontSize = 11.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
      text = value,
      fontSize = 14.sp,
      fontWeight = FontWeight.ExtraBold,
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}

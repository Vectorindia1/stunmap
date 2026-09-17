package com.stunmap.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stunmap.classifier.IpClassification
import com.stunmap.geo.GeoResult
import com.stunmap.session.IpGeoEntry
import com.stunmap.ui.theme.CandidateGreen
import com.stunmap.ui.theme.DividerColor
import com.stunmap.ui.theme.JetBrainsMonoFamily
import com.stunmap.ui.theme.MetaRed
import com.stunmap.ui.theme.PrivateGray
import com.stunmap.ui.theme.SelfAmber
import com.stunmap.ui.theme.SurfaceDark
import com.stunmap.ui.theme.SurfaceVariantDark
import com.stunmap.ui.theme.TextPrimary
import com.stunmap.ui.theme.TextSecondary

@Composable
fun IpCard(
    entry: IpGeoEntry,
    classification: IpClassification,
    modifier: Modifier = Modifier
) {
    val borderColor = when (classification) {
        IpClassification.CANDIDATE -> CandidateGreen.copy(alpha = 0.4f)
        IpClassification.META_INFRA -> MetaRed.copy(alpha = 0.3f)
        IpClassification.SELF -> SelfAmber.copy(alpha = 0.3f)
        else -> DividerColor
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderColor)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ClassificationBadge(classification)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = entry.ip,
                color = TextPrimary,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 14.sp
            )
        }

        val geo = entry.geoResult
        if (geo != null) {
            Spacer(modifier = Modifier.height(6.dp))
            GeoInfoRow(geo)
        } else if (classification == IpClassification.CANDIDATE) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "resolving location...",
                color = TextSecondary,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun GeoInfoRow(geo: GeoResult) {
    val location = listOfNotNull(geo.city, geo.region, geo.country).joinToString(", ")
    if (location.isNotEmpty()) {
        Text(
            text = location,
            color = TextSecondary,
            fontFamily = JetBrainsMonoFamily,
            fontSize = 12.sp
        )
    }
    geo.org?.let {
        Text(
            text = it,
            color = TextSecondary,
            fontFamily = JetBrainsMonoFamily,
            fontSize = 11.sp
        )
    }
    if (geo.latitude != null && geo.longitude != null) {
        Text(
            text = "[${String.format("%.4f", geo.latitude)}°, ${String.format("%.4f", geo.longitude)}°]",
            color = TextSecondary,
            fontFamily = JetBrainsMonoFamily,
            fontSize = 10.sp
        )
    }
}

package de.slg.leoapp.module.user.data

import com.fasterxml.jackson.annotation.JsonProperty

data class FeatureStatistics(val id: Long, val interactions: Int, @JsonProperty("average_time") val averageTime: Float)
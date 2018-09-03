package de.slg.leoapp.module.user.data

import com.fasterxml.jackson.annotation.JsonProperty

class PostFeatureUsage(@JsonProperty("feature_id") val featureId: Long?, @JsonProperty("usage_time") val time: Float?)
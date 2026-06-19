package com.example.pacelock.RoomDB

import androidx.room.TypeConverter
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.util.GeoPoint

class GeoPointTypeConverter {

    @TypeConverter
    fun fromGeoPointString(points: List<GeoPoint>) : String{
        val JsonArray = JSONArray()
        points.forEach {point ->
            val obj = JSONObject().apply {
                put("lat", point.latitude)
                put("lng", point.longitude)
            }
            JsonArray.put(obj)
        }
        return JsonArray.toString()
    }


    @TypeConverter
    fun toGeoPointString(json: String) : List<GeoPoint>{
        val JsonArray = JSONArray(json)
        val points = mutableListOf<GeoPoint>()
        for(i in 0 until JsonArray.length()){
            val obj = JsonArray.getJSONObject(i)
            points.add(GeoPoint(obj.getDouble("lat"), obj.getDouble("lng")))
        }
        return points
    }
}
package com.example.tianshuhe.coolweather.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by tianshuhe on 17/8/23.
 */
//
//"basic": {
//        "city": "青岛",
//        "cnty": "中国",
//        "id": "CN101120201",
//        "lat": "36.088000",
//        "lon": "120.343000",
//        "prov": "山东"  //城市所属省份（仅限国内城市）
//        "update": {
//        "loc": "2016-08-30 11:52",
//        "utc": "2016-08-30 03:52"
//        }
//        },
public class Basic {
    @SerializedName("city")
    public String cityName;

    @SerializedName("id")
    public String weatherId;

    public Update update;

    public class Update {
        @SerializedName("loc")
        public String updateTime;
    }

}

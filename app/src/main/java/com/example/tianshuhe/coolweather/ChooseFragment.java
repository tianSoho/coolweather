package com.example.tianshuhe.coolweather;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tianshuhe.coolweather.db.City;
import com.example.tianshuhe.coolweather.db.Country;
import com.example.tianshuhe.coolweather.db.Province;
import com.example.tianshuhe.coolweather.util.HttpUtil;
import com.example.tianshuhe.coolweather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by tianshuhe on 17/8/22.
 */

public class ChooseFragment extends Fragment {
    private TextView mTitleText;
    private Button mBackButton;
    private ListView mListView;
    private ProgressDialog mProgressDialog;

    private List<Province> mProvinces;
    private List<City> mCities;
    private List<Country> mCountries;
    private List<String> mDataList = new ArrayList<>();
    private ArrayAdapter<String> mAdapter;

    private static final int PROVINCE_TYPE = 0;
    private static final int CITY_TYPE = 1;
    private static final int COUNTRY_TYPE = 2;

    private static final String BASE_ADDRESS = "http://guolin.tech/api/china";

    private int mCurLevel = PROVINCE_TYPE;
    private Province mSelectProvince;
    private City mSelectCity;
    private Country mSelectCountry;

    private final String TAG = "ChooseFragment";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        mTitleText = view.findViewById(R.id.title_text);
        mBackButton = view.findViewById(R.id.title_button);
        mListView = view.findViewById(R.id.list_view);
        mProvinces = new ArrayList<>();
        mCities = new ArrayList<>();
        mCountries = new ArrayList<>();
        mDataList = new ArrayList<>();
        mAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_list_item_1, mDataList);
        mListView.setAdapter(mAdapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // 设置list点击事件
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (mCurLevel == PROVINCE_TYPE) {
                    mSelectProvince = mProvinces.get(i);
                    queryCities();
                } else if (mCurLevel == CITY_TYPE) {
                    mSelectCity = mCities.get(i);
                    queryCountries();
                } else if (mCurLevel == COUNTRY_TYPE){
                    mSelectCountry = mCountries.get(i);
                } else {
                    Log.e(TAG, "unknown level: " + mCurLevel);
                    getActivity().finish();
                }
            }
        });

        // 设置返回按钮事件
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCurLevel == PROVINCE_TYPE) {
                } else if (mCurLevel == CITY_TYPE) {
                    queryProvinces();
                } else if (mCurLevel == COUNTRY_TYPE){
                    queryCities();
                } else {
                    Log.e(TAG, "back unknown level: " + mCurLevel);
                    getActivity().finish();
                }
            }
        });

        queryProvinces();
    }

    private void queryProvinces() {
        // 首先查询数据库，如果存在则直接使用数据库的值
        mTitleText.setText("省份");
        mBackButton.setVisibility(View.GONE);
        mCurLevel = PROVINCE_TYPE;
        mProvinces = DataSupport.findAll(Province.class);
        if (mProvinces.size() > 0) {
            mDataList.clear();
            for (Province province: mProvinces) {
                mDataList.add(province.getProvinceName());
            }
            mAdapter.notifyDataSetChanged();
        } else {
            queryFromServer(BASE_ADDRESS, PROVINCE_TYPE);
        }
    }

    private void queryCities() {
        // 首先查询数据库，如果存在则直接使用数据库的值
        mTitleText.setText("城市");
        mBackButton.setVisibility(View.VISIBLE);
        mCurLevel = CITY_TYPE;
        mCities = DataSupport.where("provinceId = ?", mSelectProvince.getId() + "").
                find(City.class);
        if (mCities.size() > 0) {
            mDataList.clear();
            for (City city: mCities) {
                mDataList.add(city.getCityName());
            }
            mAdapter.notifyDataSetChanged();
        } else {
            int provinceCode = mSelectProvince.getProvinceCode();
            queryFromServer(BASE_ADDRESS + "/" + provinceCode, CITY_TYPE);
        }
    }

    private void queryCountries() {
        // 首先查询数据库，如果存在则直接使用数据库的值
        mTitleText.setText("地区");
        mBackButton.setVisibility(View.VISIBLE);
        mCurLevel = COUNTRY_TYPE;
        mCountries = DataSupport.where("cityId = ?", mSelectCity.getId() + "").
                find(Country.class);
        if (mCountries.size() > 0) {
            mDataList.clear();
            for (Country country: mCountries) {
                mDataList.add(country.getCountryName());
            }
            mAdapter.notifyDataSetChanged();
        } else {
            int provinceCode = mSelectProvince.getProvinceCode();
            int cityCode = mSelectCity.getCityCode();
            queryFromServer(BASE_ADDRESS + "/" + provinceCode + "/" + cityCode, COUNTRY_TYPE);
        }
    }

    private void queryFromServer(String address, final int type) {
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getActivity(), "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                if (type == PROVINCE_TYPE) {
                    result = Utility.handleProvinceResponce(responseText);
                } else if (type == CITY_TYPE) {
                    result = Utility.handleCityResponce(responseText, mSelectProvince.getId());
                } else if (type == COUNTRY_TYPE) {
                    result = Utility.handleCountryResponce(responseText, mSelectCity.getId());
                }
                if (result) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if (type == PROVINCE_TYPE) {
                                queryProvinces();
                            } else if (type == CITY_TYPE) {
                                queryCities();
                            } else if (type == COUNTRY_TYPE) {
                                queryCountries();
                            }
                        }
                    });
                }
            }
        });
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setMessage("正在加载");
            mProgressDialog.setCancelable(false);
        }
        mProgressDialog.show();
    }

    private void closeProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }
}

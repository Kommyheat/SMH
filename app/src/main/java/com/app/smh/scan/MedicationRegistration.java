package com.app.smh.scan;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * 스케줄러에 등록할 최종 약 정보 구조.
 * drugName: 약 이름 전체
 * category: 의약품 카테고리 (감기약, 혈압약 등)
 * startDate, endDate: "yyyy-MM-dd" 형식 또는 null
 * times: 복약 시간 리스트 (아침/점심/저녁)
 */
public class MedicationRegistration implements Parcelable {

    private String drugName;
    private String category;
    private String startDate;
    private String endDate;
    private List<MedicationTime> times;

    public MedicationRegistration(String drugName,
                                  String category,
                                  String startDate,
                                  String endDate,
                                  List<MedicationTime> times) {
        this.drugName = drugName;
        this.category = category;
        this.startDate = startDate;
        this.endDate = endDate;
        this.times = times;
    }

    protected MedicationRegistration(Parcel in) {
        drugName = in.readString();
        category = in.readString();
        startDate = in.readString();
        endDate = in.readString();

        times = new ArrayList<>();
        int size = in.readInt();
        for (int i = 0; i < size; i++) {
            String name = in.readString();
            times.add(MedicationTime.valueOf(name));
        }
    }

    public static final Creator<MedicationRegistration> CREATOR = new Creator<MedicationRegistration>() {
        @Override
        public MedicationRegistration createFromParcel(Parcel in) {
            return new MedicationRegistration(in);
        }

        @Override
        public MedicationRegistration[] newArray(int size) {
            return new MedicationRegistration[size];
        }
    };

    public String getDrugName() {
        return drugName;
    }

    public String getCategory() {
        return category;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public List<MedicationTime> getTimes() {
        return times;
    }

    public MedicationRegistration copyWith(String category,
                                           String startDate,
                                           String endDate,
                                           List<MedicationTime> times) {
        return new MedicationRegistration(
                drugName,
                category,
                startDate,
                endDate,
                times
        );
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(drugName);
        dest.writeString(category);
        dest.writeString(startDate);
        dest.writeString(endDate);
        if (times == null) {
            dest.writeInt(0);
        } else {
            dest.writeInt(times.size());
            for (MedicationTime t : times) {
                dest.writeString(t.name());
            }
        }
    }
}
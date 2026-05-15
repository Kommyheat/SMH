package com.app.smh.scan;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Gemini OCR 결과 모델
 * rawText: 이미지에서 추출한 전체 텍스트
 * drugNames: OCR에서 추출한 약품명 후보 리스트
 */
public class OcrResult implements Parcelable {

    private final String rawText;
    private final ArrayList<String> drugNames;

    public OcrResult(String rawText, ArrayList<String> drugNames) {
        this.rawText = rawText != null ? rawText : "";
        this.drugNames = drugNames != null ? new ArrayList<>(drugNames) : new ArrayList<>();
    }

    protected OcrResult(Parcel in) {
        rawText = in.readString();
        ArrayList<String> list = in.createStringArrayList();
        drugNames = list != null ? list : new ArrayList<>();
    }

    public static final Creator<OcrResult> CREATOR = new Creator<OcrResult>() {
        @Override
        public OcrResult createFromParcel(Parcel in) {
            return new OcrResult(in);
        }

        @Override
        public OcrResult[] newArray(int size) {
            return new OcrResult[size];
        }
    };

    public String getRawText() {
        return rawText;
    }

    public ArrayList<String> getDrugNames() {
        return new ArrayList<>(drugNames);
    }

    public boolean hasDrugNames() {
        return !drugNames.isEmpty();
    }

    public int getDrugCount() {
        return drugNames.size();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(rawText);
        dest.writeStringList(drugNames);
    }
}
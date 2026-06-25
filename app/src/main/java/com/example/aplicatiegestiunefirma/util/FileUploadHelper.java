package com.example.aplicatiegestiunefirma.util;

import android.content.Context;
import android.net.Uri;

import com.example.aplicatiegestiunefirma.network.ApiService;
import com.example.aplicatiegestiunefirma.network.RetrofitClient;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Trimite un fisier local (poza, PDF) pe server, ca acesta sa fie accesibil de pe orice dispozitiv. */
public class FileUploadHelper {

    public interface UploadCallback {
        void onSuccess(String fileUrl);
        void onFailure(String error);
    }

    public static void uploadFile(Context context, Uri uri, String token, UploadCallback callback) {
        new Thread(() -> {
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(uri);
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] chunk = new byte[8192];
                int read;
                while ((read = inputStream.read(chunk)) != -1) {
                    buffer.write(chunk, 0, read);
                }
                inputStream.close();

                String mimeType = context.getContentResolver().getType(uri);
                if (mimeType == null) mimeType = "application/octet-stream";
                String fileName = "upload_" + System.currentTimeMillis();

                RequestBody requestFile = RequestBody.create(buffer.toByteArray(), MediaType.parse(mimeType));
                MultipartBody.Part part = MultipartBody.Part.createFormData("file", fileName, requestFile);

                RetrofitClient.getApiService().uploadFile(token, part).enqueue(new Callback<ApiService.UploadResponse>() {
                    @Override
                    public void onResponse(Call<ApiService.UploadResponse> call, Response<ApiService.UploadResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().url != null) {
                            callback.onSuccess(RetrofitClient.getFileUrl(response.body().url));
                        } else {
                            callback.onFailure("Eroare server la incarcare fisier");
                        }
                    }
                    @Override
                    public void onFailure(Call<ApiService.UploadResponse> call, Throwable t) {
                        callback.onFailure(t.getMessage());
                    }
                });
            } catch (Exception e) {
                callback.onFailure(e.getMessage());
            }
        }).start();
    }
}

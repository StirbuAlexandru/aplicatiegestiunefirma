package com.example.aplicatiegestiunefirma.network;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class RetrofitClient {
    // Adresa fara "/" la final pentru a evita erorile 404
    private static final String BASE_URL = "https://registrational-jessenia-sleevelike.ngrok-free.dev";
    private static Retrofit retrofit = null;

    public static ApiService getApiService() {
        if (retrofit == null) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .addInterceptor(interceptor)
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        Request request = original.newBuilder()
                                .header("ngrok-skip-browser-warning", "true")
                                .build();
                        return chain.proceed(request);
                    })
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL + "/") // Retrofit cere ca BASE_URL sa se termine in "/"
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit.create(ApiService.class);
    }

    /** Transforma un URL relativ (ex: "/files/abc.jpg") in URL complet, accesibil pe orice dispozitiv. */
    public static String getFileUrl(String relativeOrAbsoluteUrl) {
        if (relativeOrAbsoluteUrl == null) return null;
        if (relativeOrAbsoluteUrl.startsWith("http")) return relativeOrAbsoluteUrl;
        return BASE_URL + relativeOrAbsoluteUrl;
    }
}

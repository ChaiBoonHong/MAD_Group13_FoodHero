package com.uccd3223.group13.foodhero.data.remote;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface SupabaseStorageService {
    @POST("/storage/v1/object/{bucket}/{path}")
    Call<ResponseBody> uploadFile(
        @Header("apikey") String apiKey,
        @Header("Authorization") String bearer,
        @Header("Content-Type") String contentType,
        @Path("bucket") String bucket,
        @Path(value = "path", encoded = true) String path,
        @Body RequestBody fileBody
    );

    @DELETE("/storage/v1/object/{bucket}/{path}")
    Call<ResponseBody> deleteFile(
        @Header("apikey") String apiKey,
        @Header("Authorization") String bearer,
        @Path("bucket") String bucket,
        @Path(value = "path", encoded = true) String path
    );
}

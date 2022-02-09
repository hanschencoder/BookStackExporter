package site.hanschen.api;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import site.hanschen.entry.*;

public interface BookStackApi {

    @GET("api/shelves")
    Call<Data> getAllShelve(@Header("Authorization") String token);

    @GET("api/books")
    Call<Data> getAllBook(@Header("Authorization") String token);

    @GET("api/chapters")
    Call<Data> getAllChapter(@Header("Authorization") String token);

    @GET("api/pages")
    Call<Data> getAllPage(@Header("Authorization") String token);

    @GET("api/shelves/{id}")
    Call<Shelve> getShelve(@Header("Authorization") String token, @Path("id") int id);

    @GET("api/books/{id}")
    Call<Book> getBook(@Header("Authorization") String token, @Path("id") int id);

    @GET("api/chapters/{id}")
    Call<Chapter> getChapter(@Header("Authorization") String token, @Path("id") int id);

    @GET("api/pages/{id}")
    Call<Page> getPage(@Header("Authorization") String token, @Path("id") int id);

    @GET("api/pages/{id}/export/{type}")
    Call<ResponseBody> getExportFile(@Header("Authorization") String token, @Path("id") int id, @Path("type") String type);

    @GET("{url}")
    Call<ResponseBody> getImage(@Path("url") String url);
}

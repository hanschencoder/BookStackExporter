package site.hanschen.entry;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Shelve {

    @SerializedName("id")
    public Integer id;
    @SerializedName("name")
    public String name;
    @SerializedName("slug")
    public String slug;
    @SerializedName("description")
    public String description;
    @SerializedName("created_by")
    public CreatedByEntry createdBy;
    @SerializedName("updated_by")
    public UpdatedByEntry updatedBy;
    @SerializedName("owned_by")
    public OwnedByEntry ownedBy;
    @SerializedName("created_at")
    public String createdAt;
    @SerializedName("updated_at")
    public String updatedAt;
    @SerializedName("tags")
    public List<TagsEntry> tags;
    @SerializedName("cover")
    public CoverEntry cover;
    @SerializedName("books")
    public List<BooksEntry> books;

    public static class CreatedByEntry {
        @SerializedName("id")
        public Integer id;
        @SerializedName("name")
        public String name;
    }

    public static class UpdatedByEntry {
        @SerializedName("id")
        public Integer id;
        @SerializedName("name")
        public String name;
    }

    public static class OwnedByEntry {
        @SerializedName("id")
        public Integer id;
        @SerializedName("name")
        public String name;
    }

    public static class CoverEntry {
        @SerializedName("id")
        public Integer id;
        @SerializedName("name")
        public String name;
        @SerializedName("url")
        public String url;
        @SerializedName("created_at")
        public String createdAt;
        @SerializedName("updated_at")
        public String updatedAt;
        @SerializedName("created_by")
        public Integer createdBy;
        @SerializedName("updated_by")
        public Integer updatedBy;
        @SerializedName("path")
        public String path;
        @SerializedName("type")
        public String type;
        @SerializedName("uploaded_to")
        public Integer uploadedTo;
    }

    public static class TagsEntry {
        @SerializedName("id")
        public Integer id;
        @SerializedName("name")
        public String name;
        @SerializedName("value")
        public String value;
        @SerializedName("order")
        public Integer order;
    }

    public static class BooksEntry {
        @SerializedName("id")
        public Integer id;
        @SerializedName("name")
        public String name;
        @SerializedName("slug")
        public String slug;
    }
}

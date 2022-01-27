package site.hanschen.entry;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Data {

    @SerializedName("data")
    public List<DataEntry> data;
    @SerializedName("total")
    public Integer total;

    public static class DataEntry {
        @SerializedName("id")
        public Integer id;
        @SerializedName("book_id")
        public Integer bookId;
        @SerializedName("chapter_id")
        public Integer chapterId;
        @SerializedName("name")
        public String name;
        @SerializedName("description")
        public String description;
        @SerializedName("slug")
        public String slug;
        @SerializedName("priority")
        public Integer priority;
        @SerializedName("draft")
        public Boolean draft;
        @SerializedName("template")
        public Boolean template;
        @SerializedName("created_at")
        public String createdAt;
        @SerializedName("updated_at")
        public String updatedAt;
        @SerializedName("created_by")
        public Integer createdBy;
        @SerializedName("updated_by")
        public Integer updatedBy;
        @SerializedName("owned_by")
        public Integer ownedBy;
    }
}

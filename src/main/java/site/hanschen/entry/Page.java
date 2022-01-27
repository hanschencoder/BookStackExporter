package site.hanschen.entry;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Page {

    @SerializedName("id")
    public Integer id;
    @SerializedName("book_id")
    public Integer bookId;
    @SerializedName("chapter_id")
    public Integer chapterId;
    @SerializedName("name")
    public String name;
    @SerializedName("slug")
    public String slug;
    @SerializedName("html")
    public String html;
    @SerializedName("priority")
    public Integer priority;
    @SerializedName("created_at")
    public String createdAt;
    @SerializedName("updated_at")
    public String updatedAt;
    @SerializedName("created_by")
    public CreatedByEntry createdBy;
    @SerializedName("updated_by")
    public UpdatedByEntry updatedBy;
    @SerializedName("owned_by")
    public OwnedByEntry ownedBy;
    @SerializedName("draft")
    public Boolean draft;
    @SerializedName("markdown")
    public String markdown;
    @SerializedName("revision_count")
    public Integer revisionCount;
    @SerializedName("template")
    public Boolean template;
    @SerializedName("tags")
    public List<TagsEntry> tags;

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

    public static class TagsEntry {
        @SerializedName("name")
        public String name;
        @SerializedName("value")
        public String value;
        @SerializedName("order")
        public Integer order;
    }


}

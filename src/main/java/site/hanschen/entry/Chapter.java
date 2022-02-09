package site.hanschen.entry;

import com.google.gson.annotations.SerializedName;
import site.hanschen.utils.Utils;

import java.util.List;

public class Chapter {

    @SerializedName("id")
    public Integer id;
    @SerializedName("book_id")
    public Integer bookId;
    @SerializedName("slug")
    public String slug;
    @SerializedName("name")
    public String name;
    @SerializedName("description")
    public String description;
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
    @SerializedName("tags")
    public List<TagsEntry> tags;
    @SerializedName("pages")
    public List<PagesEntry> pages;

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

    public static class PagesEntry {
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
        @SerializedName("priority")
        public Integer priority;
        @SerializedName("created_at")
        public String createdAt;
        @SerializedName("updated_at")
        public String updatedAt;
        @SerializedName("created_by")
        public Integer createdBy;
        @SerializedName("updated_by")
        public Integer updatedBy;
        @SerializedName("draft")
        public Boolean draft;
        @SerializedName("revision_count")
        public Integer revisionCount;
        @SerializedName("template")
        public Boolean template;
    }

    public String generateMarkDown() {
        String tagsString = "";
        if (tags != null && tags.size() > 0) {
            tagsString = "\nTags: ";
            for (int i = 0; i < tags.size(); i++) {
                Chapter.TagsEntry entry = tags.get(i);
                tagsString += "`" + entry.name + "`";
                if (i != tags.size() - 1) {
                    tagsString += ", ";
                }
            }
            tagsString += "\n";
        }
        String descriptionString = "";
        if (description.length() > 0) {
            descriptionString = "\n" + description + "\n";
        }
        return String.format("# %s\n" +
                             "%s" +
                             "%s" +
                             "\n| 属性        | 值   |\n" +
                             "|:---------:|:---:|\n" +
                             "| createdAt |  %s |\n" +
                             "| updatedAt |  %s |\n" +
                             "| createdBy |  %s |\n" +
                             "| updatedBy |  %s |\n",
                             name,
                             tagsString,
                             descriptionString,
                             Utils.formatDate(createdAt),
                             Utils.formatDate(updatedAt),
                             createdBy.name,
                             updatedBy.name);
    }
}

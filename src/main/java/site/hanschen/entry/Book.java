package site.hanschen.entry;

import com.google.gson.annotations.SerializedName;
import site.hanschen.Utils;

import java.util.List;

public class Book {


    @SerializedName("id")
    public Integer id;
    @SerializedName("name")
    public String name;
    @SerializedName("slug")
    public String slug;
    @SerializedName("description")
    public String description;
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
    @SerializedName("cover")
    public CoverEntry cover;

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

    public String generateMarkDown() {
        String tagsString = "";
        if (tags != null && tags.size() > 0) {
            tagsString = "\nTags: ";
            for (int i = 0; i < tags.size(); i++) {
                TagsEntry entry = tags.get(i);
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

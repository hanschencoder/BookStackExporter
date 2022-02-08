package site.hanschen.sync;

import com.google.gson.annotations.SerializedName;

/**
 * @author chenhang
 */
public class WebHookEvent {

    @SerializedName("event")
    public String event;
    @SerializedName("text")
    public String text;
    @SerializedName("triggered_at")
    public String triggeredAt;
    @SerializedName("triggered_by")
    public TriggeredByEntry triggeredBy;
    @SerializedName("triggered_by_profile_url")
    public String triggeredByProfileUrl;
    @SerializedName("webhook_id")
    public Integer webhookId;
    @SerializedName("webhook_name")
    public String webhookName;
    @SerializedName("url")
    public String url;
    @SerializedName("related_item")
    public RelatedItemEntry relatedItem;

    public static class TriggeredByEntry {
        @SerializedName("id")
        public Integer id;
        @SerializedName("name")
        public String name;
        @SerializedName("slug")
        public String slug;
    }

    public static class RelatedItemEntry {
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
        @SerializedName("owned_by")
        public Integer ownedBy;
    }
}

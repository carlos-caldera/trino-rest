/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pl.net.was.rest.github.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.connector.ColumnMetadata;
import pl.net.was.rest.github.GithubRest;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.trino.spi.type.BigintType.BIGINT;
import static io.trino.spi.type.BooleanType.BOOLEAN;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Issue
        extends BaseBlockWriter
{
    private final long id;
    private final String url;
    private final String eventsUrl;
    private final String htmlUrl;
    private final long number;
    private final String state;
    private final String title;
    private final String body;
    private final User user;
    private final List<Label> labels;
    private final User assignee;
    private final Milestone milestone;
    private final Boolean locked;
    private final String activeLockReason;
    private final long comments;
    private final ZonedDateTime closedAt;
    private final ZonedDateTime createdAt;
    private final ZonedDateTime updatedAt;
    private final String authorAssociation;

    public Issue(
            @JsonProperty("id") long id,
            @JsonProperty("url") String url,
            @JsonProperty("events_url") String eventsUrl,
            @JsonProperty("html_url") String htmlUrl,
            @JsonProperty("number") long number,
            @JsonProperty("state") String state,
            @JsonProperty("title") String title,
            @JsonProperty("body") String body,
            @JsonProperty("user") User user,
            @JsonProperty("labels") List<Label> labels,
            @JsonProperty("assignee") User assignee,
            @JsonProperty("milestone") Milestone milestone,
            @JsonProperty("locked") Boolean locked,
            @JsonProperty("active_lock_reason") String activeLockReason,
            @JsonProperty("comments") long comments,
            @JsonProperty("closed_at") ZonedDateTime closedAt,
            @JsonProperty("created_at") ZonedDateTime createdAt,
            @JsonProperty("updated_at") ZonedDateTime updatedAt,
            @JsonProperty("author_association") String authorAssociation)
    {
        this.id = id;
        this.url = url;
        this.eventsUrl = eventsUrl;
        this.htmlUrl = htmlUrl;
        this.number = number;
        this.state = state;
        this.title = title;
        this.body = body;
        this.user = user;
        this.labels = labels;
        this.assignee = assignee;
        this.milestone = milestone;
        this.locked = locked;
        this.activeLockReason = activeLockReason;
        this.comments = comments;
        this.closedAt = closedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.authorAssociation = authorAssociation;
    }

    public List<?> toRow()
    {
        return ImmutableList.of(
                id,
                number,
                state,
                title,
                body,
                user.getId(),
                user.getLogin(),
                labels.stream().map(Label::getId).collect(Collectors.toList()),
                labels.stream().map(Label::getName).collect(Collectors.toList()),
                assignee.getId(),
                assignee.getLogin(),
                milestone.getId(),
                milestone.getTitle(),
                locked,
                activeLockReason,
                comments,
                closedAt,
                createdAt,
                updatedAt,
                authorAssociation);
    }

    @Override
    public void writeTo(BlockBuilder rowBuilder)
    {
        // TODO this should be a map of column names to value getters and types should be fetched from GithubRest.columns
        BIGINT.writeLong(rowBuilder, id);
        BIGINT.writeLong(rowBuilder, number);
        writeString(rowBuilder, state);
        writeString(rowBuilder, title);
        writeString(rowBuilder, body);
        BIGINT.writeLong(rowBuilder, user.getId());
        writeString(rowBuilder, user.getLogin());

        // labels array
        Map<String, ColumnMetadata> columns = GithubRest.columns.get("issues").stream()
                .collect(Collectors.toMap(ColumnMetadata::getName, columnMetadata -> columnMetadata));
        BlockBuilder labelIds = columns.get("label_ids").getType().createBlockBuilder(null, labels.size());
        for (Label label : labels) {
            labelIds.writeLong(label.getId());
        }
        rowBuilder.appendStructure(labelIds.build());

        BlockBuilder labelNames = columns.get("label_names").getType().createBlockBuilder(null, labels.size());
        for (Label label : labels) {
            writeString(labelNames, label.getName());
        }
        rowBuilder.appendStructure(labelNames.build());

        BIGINT.writeLong(rowBuilder, assignee.getId());
        writeString(rowBuilder, assignee.getLogin());
        BIGINT.writeLong(rowBuilder, milestone.getId());
        writeString(rowBuilder, milestone.getTitle());
        BOOLEAN.writeBoolean(rowBuilder, locked);
        writeString(rowBuilder, activeLockReason);
        BIGINT.writeLong(rowBuilder, comments);
        writeTimestamp(rowBuilder, closedAt);
        writeTimestamp(rowBuilder, createdAt);
        writeTimestamp(rowBuilder, updatedAt);
        writeString(rowBuilder, authorAssociation);
    }
}
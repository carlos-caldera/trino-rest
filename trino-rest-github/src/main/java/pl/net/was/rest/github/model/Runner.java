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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import io.trino.spi.block.BlockBuilder;

import java.util.List;

import static io.trino.spi.type.BigintType.BIGINT;
import static io.trino.spi.type.BooleanType.BOOLEAN;
import static io.trino.spi.type.VarcharType.VARCHAR;

public class Runner
        extends BaseBlockWriter
{
    private String org;
    private String owner;
    private String repo;
    private final long id;
    private final String name;
    private final String os;
    private final String status;
    private final boolean busy;
    private final List<Label> labels;

    public Runner(
            @JsonProperty("id") long id,
            @JsonProperty("name") String name,
            @JsonProperty("os") String os,
            @JsonProperty("status") String status,
            @JsonProperty("busy") boolean busy,
            @JsonProperty("labels") List<Label> labels)
    {
        this.id = id;
        this.name = name;
        this.os = os;
        this.status = status;
        this.busy = busy;
        this.labels = labels;
    }

    public void setOrg(String org)
    {
        this.org = org;
    }

    public void setOwner(String owner)
    {
        this.owner = owner;
    }

    public void setRepo(String repo)
    {
        this.repo = repo;
    }

    public List<?> toRow()
    {
        BlockBuilder labelIds = BIGINT.createBlockBuilder(null, labels.size());
        BlockBuilder labelNames = VARCHAR.createBlockBuilder(null, labels.size());
        for (Label label : labels) {
            BIGINT.writeLong(labelIds, label.getId());
            VARCHAR.writeString(labelNames, label.getName());
        }

        return ImmutableList.of(
                org != null ? org : "",
                owner != null ? owner : "",
                repo != null ? repo : "",
                id,
                name,
                os != null ? os : "",
                status,
                busy,
                labelIds.build(),
                labelNames.build());
    }

    @Override
    public void writeTo(List<BlockBuilder> fieldBuilders)
    {
        int i = 0;
        writeString(fieldBuilders.get(i++), org);
        writeString(fieldBuilders.get(i++), owner);
        writeString(fieldBuilders.get(i++), repo);
        BIGINT.writeLong(fieldBuilders.get(i++), id);
        VARCHAR.writeString(fieldBuilders.get(i++), name);
        VARCHAR.writeString(fieldBuilders.get(i++), os);
        VARCHAR.writeString(fieldBuilders.get(i++), status);
        BOOLEAN.writeBoolean(fieldBuilders.get(i++), busy);
        if (labels == null) {
            fieldBuilders.get(i++).appendNull();
            fieldBuilders.get(i).appendNull();
        }
        else {
            // labels array
            BlockBuilder labelIds = BIGINT.createBlockBuilder(null, labels.size());
            for (Label label : labels) {
                BIGINT.writeLong(labelIds, label.getId());
            }
            ARRAY_BIGINT.writeObject(fieldBuilders.get(i++), labelIds.build());

            BlockBuilder labelNames = VARCHAR.createBlockBuilder(null, labels.size());
            for (Label label : labels) {
                writeString(labelNames, label.getName());
            }
            ARRAY_VARCHAR.writeObject(fieldBuilders.get(i), labelNames.build());
        }
    }
}

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

package pl.net.was.rest.github.function;

import com.google.common.collect.ImmutableList;
import io.airlift.slice.Slice;
import io.trino.spi.PageBuilder;
import io.trino.spi.block.Block;
import io.trino.spi.function.Description;
import io.trino.spi.function.ScalarFunction;
import io.trino.spi.function.SqlType;
import io.trino.spi.type.ArrayType;
import io.trino.spi.type.RowType;
import pl.net.was.rest.github.GithubRest;
import pl.net.was.rest.github.model.Pull;
import retrofit2.Response;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static io.trino.spi.type.StandardTypes.INTEGER;
import static io.trino.spi.type.StandardTypes.VARCHAR;
import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static pl.net.was.rest.github.GithubRest.PULLS_TABLE_TYPE;

@ScalarFunction("pulls")
@Description("Get pull requests")
public class Pulls
        extends BaseFunction
{
    public Pulls()
    {
        List<RowType.Field> fields = GithubRest.columns.get("pulls")
                .stream()
                .map(columnMetadata -> RowType.field(
                        columnMetadata.getName(),
                        columnMetadata.getType()))
                .collect(Collectors.toList());
        RowType rowType = RowType.from(fields);

        arrayType = new ArrayType(rowType);
        pageBuilder = new PageBuilder(ImmutableList.of(arrayType));
    }

    @SqlType(PULLS_TABLE_TYPE)
    public Block getPage(@SqlType(VARCHAR) Slice token, @SqlType(VARCHAR) Slice owner, @SqlType(VARCHAR) Slice repo, @SqlType(INTEGER) long page)
            throws IOException
    {
        Response<List<Pull>> response = service.listPulls(
                token.toStringUtf8(),
                owner.toStringUtf8(),
                repo.toStringUtf8(),
                100,
                (int) page).execute();
        if (response.code() == HTTP_NOT_FOUND) {
            return null;
        }
        if (!response.isSuccessful()) {
            throw new IllegalStateException(format("Invalid response, code %d, message: %s", response.code(), response.message()));
        }
        List<Pull> items = response.body();
        return buildBlock(items);
    }
}
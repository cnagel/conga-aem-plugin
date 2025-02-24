/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2015 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.devops.conga.plugins.aem.fileheader;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

import io.wcm.devops.conga.generator.spi.FileHeaderPlugin;
import io.wcm.devops.conga.generator.spi.context.FileContext;
import io.wcm.devops.conga.generator.spi.context.FileHeaderContext;
import io.wcm.devops.conga.generator.util.PluginManagerImpl;

class AnyFileHeaderTest {

  private FileHeaderPlugin underTest;

  @BeforeEach
  void setUp() {
    underTest = new PluginManagerImpl().get(AnyFileHeader.NAME, FileHeaderPlugin.class);
  }

  @Test
  void testApply() throws Exception {
    File file = new File("target/generation-test/fileHeader.any");
    FileUtils.write(file, "myany { }", StandardCharsets.UTF_8);

    List<String> lines = ImmutableList.of("Der Jodelkaiser", "aus dem Oetztal", "ist wieder daheim.");
    FileHeaderContext context = new FileHeaderContext().commentLines(lines);
    FileContext fileContext = new FileContext().file(file);

    assertTrue(underTest.accepts(fileContext, context));
    underTest.apply(fileContext, context);

    assertTrue(StringUtils.contains(FileUtils.readFileToString(file, StandardCharsets.UTF_8),
        "# Der Jodelkaiser\n# aus dem Oetztal\n# ist wieder daheim.\n"));

    FileHeaderContext extractContext = underTest.extract(fileContext);
    assertEquals(lines, extractContext.getCommentLines());

    file.delete();
  }

}

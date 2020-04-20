/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2020 wcm.io
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
package io.wcm.devops.conga.plugins.aem.maven.model;

import static io.wcm.devops.conga.generator.util.FileUtil.getCanonicalPath;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;

import io.wcm.devops.conga.model.util.MapExpander;
import io.wcm.devops.conga.plugins.aem.postprocessor.ContentPackagePropertiesPostProcessor;

/**
 * Parsers model.yaml files generated by CONGA.
 */
public final class ModelParser {

  /**
   * Model file.
   */
  public static final String MODEL_FILE = "model.yaml";

  private final Yaml yaml;

  /**
   * Constructor
   */
  public ModelParser() {
    this.yaml = YamlUtil.createYaml();
  }

  /**
   * Parses model.yaml file for given node and returns all content packages references in this fileData.
   * @param nodeDir Node directory
   * @return List of content packages
   */
  public List<ContentPackageFile> getContentPackagesForNode(File nodeDir) {
    Map<String, Object> data = getModelData(nodeDir);
    return collectPackages(data, nodeDir);
  }

  /**
   * Checks if the node has the given node role assigned.
   * @param nodeDir Node directory
   * @param roleName Node role name
   * @return true if role is assigned
   */
  @SuppressWarnings("unchecked")
  public boolean hasRole(File nodeDir, String roleName) {
    Map<String, Object> data = getModelData(nodeDir);
    List<Map<String, Object>> roles = (List<Map<String, Object>>)data.get("roles");
    for (Map<String, Object> role : roles) {
      if (StringUtils.equals((String)role.get("role"), roleName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Collects all assigned "cloudManager.target" values (lists or single values) to any role from the node.
   * @param nodeDir Node directory
   * @return List of cloud manager environment names or "none"
   */
  @SuppressWarnings("unchecked")
  public Set<String> getCloudManagerTarget(File nodeDir) {
    Set<String> targets = new LinkedHashSet<>();
    Map<String, Object> data = getModelData(nodeDir);
    List<Map<String, Object>> roles = (List<Map<String, Object>>)data.get("roles");
    for (Map<String, Object> role : roles) {
      Map<String, Object> config = (Map<String, Object>)role.get("config");
      if (config != null) {
        Object targetValue = MapExpander.getDeep(config, "cloudManager.target");
        if (targetValue != null) {
          if (targetValue instanceof String) {
            String target = (String)targetValue;
            if (!StringUtils.isBlank(target)) {
              targets.add(target);
            }
          }
          else if (targetValue instanceof List) {
            targets.addAll(((List<String>)targetValue).stream()
                .filter(target -> !StringUtils.isBlank(target))
                .collect(Collectors.toList()));
          }
          else {
            throw new RuntimeException("Invalid cloudManager.target value: " + targetValue);
          }
        }
      }
    }
    return targets;
  }

  private Map<String, Object> getModelData(File nodeDir) {
    File modelFile = new File(nodeDir, MODEL_FILE);
    if (!modelFile.exists() || !modelFile.isFile()) {
      throw new RuntimeException("Model file not found: " + getCanonicalPath(modelFile));
    }
    return parseYaml(modelFile);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> parseYaml(File modelFile) {
    try {
      try (InputStream is = new FileInputStream(modelFile);
          Reader reader = new InputStreamReader(is, CharEncoding.UTF_8)) {
        return yaml.loadAs(reader, Map.class);
      }
    }
    catch (IOException ex) {
      throw new RuntimeException("Unable to parse " + getCanonicalPath(modelFile), ex);
    }
  }

  @SuppressWarnings("unchecked")
  private List<ContentPackageFile> collectPackages(Map<String, Object> data, File nodeDir) {
    List<ContentPackageFile> items = new ArrayList<>();
    List<Map<String, Object>> roles = (List<Map<String, Object>>)data.get("roles");
    if (roles != null) {
      for (Map<String, Object> role : roles) {
        List<Map<String, Object>> files = (List<Map<String, Object>>)role.get("files");
        if (files != null) {
          for (Map<String, Object> file : files) {
            if (file.get(ContentPackagePropertiesPostProcessor.MODEL_OPTIONS_PROPERTY) != null) {
              items.add(toContentPackageFile(file, role, nodeDir));
            }
          }
        }
      }
    }
    return items;
  }

  private ContentPackageFile toContentPackageFile(Map<String, Object> fileData,
      Map<String, Object> roleData, File nodeDir) {
    String path = (String)fileData.get("path");
    File file = new File(nodeDir, path);
    return new ContentPackageFile(file, fileData, roleData);
  }

}

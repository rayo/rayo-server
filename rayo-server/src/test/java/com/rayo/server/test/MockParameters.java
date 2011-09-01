/**
 * Copyright 2010 Voxeo Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.rayo.server.test;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.media.mscontrol.Parameter;
import javax.media.mscontrol.Parameters;

public class MockParameters implements Parameters {

  private Map<Parameter, Object> map;

  @Override
  public void clear() {

  }

  @Override
  public boolean containsKey(Object arg0) {

    return false;
  }

  @Override
  public boolean containsValue(Object arg0) {

    return false;
  }

  @Override
  public Set<java.util.Map.Entry<Parameter, Object>> entrySet() {

    return null;
  }

  @Override
  final public Object get(Object arg0) {

    return map.get(arg0);
  }

  @Override
  public boolean isEmpty() {

    return false;
  }

  @Override
  public Set<Parameter> keySet() {

    return null;
  }

  @Override
  final public Object put(Parameter arg0, Object arg1) {
    if (map == null) {
      map = new ConcurrentHashMap<Parameter, Object>();
    }
    map.put(arg0, arg1);

    return null;
  }

  @Override
  public void putAll(Map<? extends Parameter, ? extends Object> arg0) {

  }

  @Override
  final public Object remove(Object arg0) {
    return map.remove(arg0);
  }

  @Override
  public int size() {

    return 0;
  }

  @Override
  public Collection<Object> values() {

    return null;
  }

}

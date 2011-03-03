/*
 * Copyright 2000-2010 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.vcs;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.util.PairProcessor;

import java.util.*;

/**
 * @author irengrig
 */
public class MembershipMap<Key, Val> extends AreaMap<Key, Val> implements MembershipMapI<Key,Val> {
  public static<Key extends Comparable<Key>, Val> MembershipMap<Key, Val> createMembershipMap(final PairProcessor<Key, Key> keysResemblance) {
    return new MembershipMap<Key,Val>(keysResemblance, new ComparableComparator<Key>());
  }

  public static<Key, Val> MembershipMap<Key, Val> createMembershipMap(final PairProcessor<Key, Key> keysResemblance, final Comparator<Key> comparator) {
    return new MembershipMap<Key,Val>(keysResemblance, comparator);
  }

  private MembershipMap(final PairProcessor<Key, Key> keysResemblance, final Comparator<Key> comparator) {
    super(keysResemblance, comparator);
  }

  @Override
  public void putOptimal(final Key key, final Val val) {
    final int idx = putImpl(key, val);

    final List<Integer> toRemove = new LinkedList<Integer>();
    // go for parents
    for (int i = idx - 1; i >= 0; -- i) {
      if (myKeysResemblance.process(myKeys.get(i), key)) {
        toRemove.add(idx);
        break;
      }
    }
    if (toRemove.isEmpty()) {
      for (int i = idx + 1; i < myKeys.size(); ++ i) {
        if (myKeysResemblance.process(key, myKeys.get(i))) {
          toRemove.add(i);
        } else {
          break;
        }
      }
    }
    Collections.sort(toRemove);
    for (int i = 0; i < toRemove.size(); i++) {
      final Integer integer = toRemove.get(i);
      final int correctedIdx = integer - i;
      final Key keyToRemove = myKeys.remove(correctedIdx);
      myMap.remove(keyToRemove);
    }
  }

  @Override
  public void optimizeMap(final PairProcessor<Val, Val> valuesAreas) {
    int i = 0;
    for (Iterator<Key> iterator = myKeys.iterator(); iterator.hasNext();) {
      final Key key = iterator.next();
      final Val value = myMap.get(key);

      // go for parents
      for (int j = i - 1; j >= 0; -- j) {
        final Key innerKey = myKeys.get(j);
        if (myKeysResemblance.process(innerKey, key)) {
          if (valuesAreas.process(myMap.get(innerKey), value)) {
            -- i;
            iterator.remove();
            myMap.remove(key);
          }
          // otherwise we found a "parent", and do not remove the child
          break;
        }
      }
      ++ i;
    }
  }

  @Override
  public Pair<Key, Val> getMapping(final Key key) {
    final Ref<Pair<Key, Val>> result = new Ref<Pair<Key, Val>>();
    getSimiliar(key, new PairProcessor<Key, Val>() {
      @Override
      public boolean process(final Key key, final Val val) {
        result.set(new Pair<Key,Val>(key, val));
        return true;
      }
    });
    return result.get();
  }
}

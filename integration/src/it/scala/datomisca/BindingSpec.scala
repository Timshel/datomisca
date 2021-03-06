/*
 * Copyright 2012 Pellucid and Zenexity
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datomisca

import org.scalatest.{FlatSpec, Matchers}


class BindingSpec
  extends FlatSpec
     with Matchers
     with DatomicFixture
{

  "A Query" can "bind variables" in {
    val query = Query("""
      [:find ?first ?last
       :in ?first ?last]
    """)

    val res = Datomic.q(query, DString("John"), DString("Doe"))

    res should have size (1)
    res.head should equal ((DString("John"), DString("Doe")))
  }

  it can "bind tuples" in {
    val query = Query("""
      [:find ?first ?last
       :in [?first ?last]]
    """)

    val res = Datomic.q(query, DColl(DString("John"), DString("Doe")))

    res should have size (1)
    res.head should equal ((DString("John"), DString("Doe")))
  }

  it can "bind a relation" in {
    val query = Query("""
      [:find ?first ?last
       :in [[?first ?last]]]
    """)

    val res =
      Datomic.q(query,
        DColl(
          DColl(DString("John"), DString("Doe")),
          DColl(DString("Jane"), DString("Doe"))
        )
      )

    res should have size (2)
    res should contain ((DString("John"), DString("Doe")))
    res should contain ((DString("Jane"), DString("Doe")))
  }

  it can "bind a database" in {
    val query = Query("""
      [:find ?first
       :where [_ :first-name ?first]]
    """)

    val res =
      Datomic.q(query,
        DColl(
          DColl(DLong(42), DKeyword(Datomic.KW(":first-name")), DString("John")),
          DColl(DLong(42), DKeyword(Datomic.KW(":last-name")),  DString("Doe")),
          DColl(DLong(43), DKeyword(Datomic.KW(":first-name")), DString("Jane")),
          DColl(DLong(43), DKeyword(Datomic.KW(":last-name")),  DString("Doe"))
        )
      )

    res should have size (2)
    res should contain (DString("John"))
    res should contain (DString("Jane"))
  }
}

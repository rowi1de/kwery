/*
 * Copyright (c) 2015 Andrew O'Malley
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.github.andrewoma.kwery.mappertest

import com.github.andrewoma.kwery.core.ManualTransaction
import kotlin.properties.Delegates
import com.github.andrewoma.kwery.core.Session
import org.junit.Before as before
import com.github.andrewoma.kwery.core.DefaultSession
import com.github.andrewoma.kwery.core.interceptor.LoggingInterceptor
import org.junit.After as after
import org.apache.tomcat.jdbc.pool.DataSource
import com.github.andrewoma.kwery.core.dialect.HsqlDialect
import org.junit.rules.TestName
import org.junit.Rule as rule
import com.github.andrewoma.kwery.core.dialect.Dialect

private val testDataSource: DataSource by Delegates.lazy {
    val dataSource = DataSource()
    dataSource.setDefaultAutoCommit(true)
    dataSource.setDriverClassName("org.hsqldb.jdbc.JDBCDriver")
    dataSource.setUrl("jdbc:hsqldb:mem:kwerydao")
    dataSource
}

abstract class AbstractSessionTest(val dataSource: javax.sql.DataSource = testDataSource, val dialect: Dialect = HsqlDialect()) {
    class object {
        val initialised: MutableSet<String> = hashSetOf()
    }

    var transaction: ManualTransaction by Delegates.notNull()
    var session: Session by Delegates.notNull()

    open var startTransactionByDefault: Boolean = true
    open var rollbackTransactionByDefault: Boolean = false

    val name = TestName()
    rule public fun name(): TestName = name // Annotating val directly doesn't work

    before public fun setUp() {
        session = DefaultSession(dataSource.getConnection(), dialect, listOf(LoggingInterceptor()))
        if (startTransactionByDefault) {
            transaction = session.manualTransaction()
        }

        afterSessionSetup()
        println("==== Starting '${name.getMethodName()}'")
    }

    open fun afterSessionSetup() {
    }

    fun initialise(token: String, f: () -> Unit) {
        if (initialised.contains(token)) return
        initialised.add(token)
        f()
    }

    after public fun tearDown() {
        try {
            if (startTransactionByDefault) {
                if (rollbackTransactionByDefault || transaction.rollbackOnly) {
                    transaction.rollback()
                } else {
                    transaction.commit()
                }
            }
        } finally {
            session.connection.close()
        }
    }
}

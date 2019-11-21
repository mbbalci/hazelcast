/*
 * Copyright (c) 2008-2019, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.sql.optimizer;

import com.hazelcast.sql.impl.calcite.schema.HazelcastSchema;
import com.hazelcast.sql.impl.calcite.schema.HazelcastTable;
import com.hazelcast.sql.impl.calcite.statistics.TableStatistics;
import com.hazelcast.sql.optimizer.support.PhysicalOptimizerTestSupport;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.schema.Table;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.fail;

/**
 * Tests of TPC-H benchmark queries.
 */
@SuppressWarnings("checkstyle:OperatorWrap")
public class TpcHPhysicalPlanningTest extends PhysicalOptimizerTestSupport {

    private static final long SCALE_FACTOR = 1L;

    private static final long PART_CNT = SCALE_FACTOR * 200_000L;
    private static final long SUPPLIER_CNT = SCALE_FACTOR * 10_000L;
    private static final long PARTSUPP_CNT = SCALE_FACTOR * 800_000L;
    private static final long CUSTOMER_CNT = SCALE_FACTOR * 150_000L;
    private static final long LINEITEM_CNT = SCALE_FACTOR * 6_000_000L;
    private static final long ORDERS_CNT = SCALE_FACTOR * 1_500_000L;
    private static final long NATION_CNT = 25;
    private static final long REGION_CNT = 5;

    @Override
    protected HazelcastSchema createDefaultSchema() {
        Map<String, Table> tableMap = new HashMap<>();

        HazelcastTable partTable = new HazelcastTable("part", true, null, null, null, new TableStatistics(PART_CNT));
        HazelcastTable supplierTable = new HazelcastTable("supplier", true, null, null, null, new TableStatistics(SUPPLIER_CNT));
        HazelcastTable partsuppTable = new HazelcastTable("partsupp", true, null, null, null, new TableStatistics(PARTSUPP_CNT));
        HazelcastTable customerTable = new HazelcastTable("customer", true, null, null, null, new TableStatistics(CUSTOMER_CNT));
        HazelcastTable lineitemTable = new HazelcastTable("lineitem", true, null, null, null, new TableStatistics(LINEITEM_CNT));
        HazelcastTable ordersTable = new HazelcastTable("orders", true, null, null, null, new TableStatistics(ORDERS_CNT));
        HazelcastTable nationTable = new HazelcastTable("nation", false, null, null, null, new TableStatistics(NATION_CNT));
        HazelcastTable regionTable = new HazelcastTable("region", false, null, null, null, new TableStatistics(REGION_CNT));

        tableMap.put(partTable.getName(), partTable);
        tableMap.put(supplierTable.getName(), supplierTable);
        tableMap.put(partsuppTable.getName(), partsuppTable);
        tableMap.put(customerTable.getName(), customerTable);
        tableMap.put(lineitemTable.getName(), lineitemTable);
        tableMap.put(ordersTable.getName(), ordersTable);
        tableMap.put(nationTable.getName(), nationTable);
        tableMap.put(regionTable.getName(), regionTable);

        return new HazelcastSchema(tableMap);
    }

    // TODO: Assert with grouping sets
    @Test
    public void testQ1() {
        RelNode rootInput = optimizePhysical(
        "select\n" +
            "    l_returnflag,\n" +
            "    l_linestatus,\n" +
            "    sum(l_quantity) as sum_qty,\n" +
            "    sum(l_extendedprice) as sum_base_price,\n" +
            "    sum(l_extendedprice*(1-l_discount)) as sum_disc_price,\n" +
            "    sum(l_extendedprice*(1-l_discount)*(1+l_tax)) as sum_charge,\n" +
            "    avg(l_quantity) as avg_qty,\n" +
            "    avg(l_extendedprice) as avg_price,\n" +
            "    avg(l_discount) as avg_disc,\n" +
            "    count(*) as count_order\n" +
            "from\n" +
            "    lineitem\n" +
            "where\n" +
            "    l_shipdate <= date '1998-12-01'\n" +
//            "    l_shipdate <= date '1998-12-01' - interval '[DELTA]' day (3)\n" + // TODO
            "group by\n" +
            "    l_returnflag,\n" +
            "    l_linestatus\n" +
            "order by\n" +
            "    l_returnflag,\n" +
            "    l_linestatus"
        );

        return;
    }

    // TODO: Corellated scalar subquery - analyze it!
    // TODO: Hash agg - root cause of failure
    @Test
    public void testQ2() {
        RelNode rootInput = optimizePhysical(
            "select\n" +
                "    s.s_acctbal,\n" +
                "    s.s_name,\n" +
                "    n.n_name,\n" +
                "    p.p_partkey,\n" +
                "    p.p_mfgr,\n" +
                "    s.s_address,\n" +
                "    s.s_phone,\n" +
                "    s.s_comment\n" +
                "from\n" +
                "    part p,\n" +
                "    supplier s,\n" +
                "    partsupp ps,\n" +
                "    nation n,\n" +
                "    region r\n" +
                "where\n" +
                "    p.p_partkey = ps.ps_partkey\n" +
                "    and s.s_suppkey = ps.ps_suppkey\n" +
                "    and p.p_size = 10\n" +
//                "    and p.p_size = [SIZE]\n" + // TODO
                "    and p.p_type like '%[TYPE]'\n" +
                "    and s.s_nationkey = n.n_nationkey\n" +
                "    and n.n_regionkey = r.r_regionkey\n" +
                "    and r.r_name = '[REGION]'\n" +
                "    and ps.ps_supplycost = (        \n" +
                "        select \n" +
                "            min(ps2.ps_supplycost)\n" +
                "        from\n" +
                "            partsupp ps2, supplier s2,\n" +
                "            nation n2, region r2\n" +
                "        where\n" +
                "            p.p_partkey = ps2.ps_partkey\n" +
                "            and s2.s_suppkey = ps2.ps_suppkey\n" +
                "            and s2.s_nationkey = n2.n_nationkey\n" +
                "            and n2.n_regionkey = r2.r_regionkey\n" +
                "            and r2.r_name = '[REGION]'\n" +
                "    )\n" +
                "order by\n" +
                "    s.s_acctbal desc,\n" +
                "    n.n_name,\n" +
                "    s.s_name,\n" +
                "    p.p_partkey"
        );

        return;
    }

    // TODO: Assert with grouping sets
    @Test
    public void testQ3() {
        RelNode rootInput = optimizePhysical(
            "select\n" +
                "    l.l_orderkey,\n" +
                "    sum(l.l_extendedprice*(1-l.l_discount)) as revenue,\n" +
                "    o.o_orderdate,\n" +
                "    o.o_shippriority\n" +
                "from\n" +
                "    customer c,\n" +
                "    orders o,\n" +
                "    lineitem l\n" +
                "where\n" +
                "    c.c_mktsegment = '[SEGMENT]'\n" +
                "    and c.c_custkey = o.o_custkey\n" +
                "    and l.l_orderkey = o.o_orderkey\n" +
                "    and o.o_orderdate < date '1998-12-01'\n" +
                "    and l.l_shipdate > date '1998-12-01'\n" +
//                "    and o.o_orderdate < date '[DATE]'\n" + // TODO
//                "    and l.l_shipdate > date '[DATE]'\n" +
                "group by\n" +
                "    l.l_orderkey,\n" +
                "    o.o_orderdate,\n" +
                "    o.o_shippriority\n" +
                "order by\n" +
                "    revenue desc,\n" +
                "    o.o_orderdate"
        );

        return;
    }

    // TODO: "EXISTS" is not processed in the condition, LogicalAggregate is not created (because hash agg is not implemented yet)
    // TODO: See HiveRelDecorrelator, RelDecorrelator
    @Test
    public void testQ4() {
        RelNode rootInput = optimizePhysical(
            "select\n" +
                "    o.o_orderpriority,\n" +
                "    count(*) as order_count\n" +
                "from\n" +
                "    orders o\n" +
                "where\n" +
                "    o.o_orderdate >= date '1998-12-01'\n" +
                "    and o.o_orderdate < date '1999-03-01'\n" +
//                "    o.o_orderdate >= date '[DATE]'\n" + // TODO
//                "    and o.o_orderdate < date '[DATE]' + interval '3' month\n" +
                "    and exists (\n" +
                "        select\n" +
                "            *\n" +
                "        from\n" +
                "            lineitem l\n" +
                "        where\n" +
                "            l.l_orderkey = o.o_orderkey\n" +
                "            and l.l_commitdate < l.l_receiptdate\n" +
                "    )\n" +
                "group by\n" +
                "    o.o_orderpriority\n" +
                "order by\n" +
                "    o.o_orderpriority"
        );

        return;
    }

    // TODO: Hash agg
    @Test
    public void testQ5() {
        RelNode rootInput = optimizePhysical(
            "select\n" +
                "    n.n_name,\n" +
                "    sum(l.l_extendedprice * (1 - l.l_discount)) as revenue\n" +
                "from\n" +
                "    customer c,\n" +
                "    orders o,\n" +
                "    lineitem l,\n" +
                "    supplier s,\n" +
                "    nation n,\n" +
                "    region r\n" +
                "where\n" +
                "    c.c_custkey = o.o_custkey\n" +
                "    and l.l_orderkey = o.o_orderkey\n" +
                "    and l.l_suppkey = s.s_suppkey\n" +
                "    and c.c_nationkey = s.s_nationkey\n" +
                "    and s.s_nationkey = n.n_nationkey\n" +
                "    and n.n_regionkey = r.r_regionkey\n" +
                "    and r.r_name = '[REGION]'\n" +
                "    and o.o_orderdate >= date '1998-12-01'\n" +
                "    and o.o_orderdate < date '1999-12-01'\n" +
//                "    and o.o_orderdate >= date '[DATE]'\n" + // TODO
//                "    and o.o_orderdate < date '[DATE]' + interval '1' year\n" +
                "group by\n" +
                "    n.n_name\n" +
                "order by\n" +
                "    revenue desc"
        );

        return;
    }

    // TODO: Hash agg
    @Test
    public void testQ6() {
        RelNode rootInput = optimizePhysical(
            "select\n" +
                "    sum(l.l_extendedprice*l.l_discount) as revenue\n" +
                "from\n" +
                "    lineitem l\n" +
                "where\n" +
                "    l.l_shipdate >= date '1998-12-01'\n" +
                "    and l.l_shipdate < date '1999-12-01'\n" +
                "    and l.l_discount between 1 - 0.01 AND 1 + 0.01\n" +
                "    and l.l_quantity < 50"
//                "    l.l_shipdate >= date '[DATE]'\n" + // TODO
//                "    and l.l_shipdate < date '[DATE]' + interval '1' year\n" +
//                "    and l.l_discount between [DISCOUNT] - 0.01 and [DISCOUNT] + 0.01\n" +
//                "    and l.l_quantity < [QUANTITY]"
        );

        return;
    }

    // TODO: Assert with grouping sets
    @Test
    public void testQ7() {
        RelNode rootInput = optimizePhysical(
            "select\n" +
                "    supp_nation,\n" +
                "    cust_nation,\n" +
                "    l_year, sum(volume) as revenue\n" +
                "from (\n" +
                "    select\n" +
                "        n1.n_name as supp_nation,\n" +
                "        n2.n_name as cust_nation,\n" +
                "        extract(year from l.l_shipdate) as l_year,\n" +
                "        l.l_extendedprice * (1 - l.l_discount) as volume\n" +
                "    from\n" +
                "        supplier s,\n" +
                "        lineitem l,\n" +
                "        orders o,\n" +
                "        customer c,\n" +
                "        nation n1,\n" +
                "        nation n2\n" +
                "    where\n" +
                "        s.s_suppkey = l.l_suppkey\n" +
                "        and o.o_orderkey = l.l_orderkey\n" +
                "        and c.c_custkey = o.o_custkey\n" +
                "        and s.s_nationkey = n1.n_nationkey\n" +
                "        and c.c_nationkey = n2.n_nationkey\n" +
                "        and (\n" +
                "            (n1.n_name = '[NATION1]' and n2.n_name = '[NATION2]')\n" +
                "            or (n1.n_name = '[NATION2]' and n2.n_name = '[NATION1]')\n" +
                "        )\n" +
                "        and l.l_shipdate between date '1995-01-01' and date '1996-12-31'\n" +
                "    ) as shipping\n" +
                "group by\n" +
                "    supp_nation,\n" +
                "    cust_nation,\n" +
                "    l_year\n" +
                "order by\n" +
                "    supp_nation,\n" +
                "    cust_nation,\n" +
                "    l_year"
        );

        return;
    }

    // TODO: Hash agg
    @Test
    public void testQ8() {
        RelNode rootInput = optimizePhysical(
            "select\n" +
                "    o_year,\n" +
                "    sum(case\n" +
                "        when nation = '[NATION]'\n" +
                "        then volume\n" +
                "        else 0\n" +
                "    end) / sum(volume) as mkt_share\n" +
                "from (\n" +
                "    select\n" +
                "        extract(year from o.o_orderdate) as o_year,\n" +
                "        l.l_extendedprice * (1 - l.l_discount) as volume,\n" +
                "        n2.n_name as nation\n" +
                "    from\n" +
                "        part p,\n" +
                "        supplier s,\n" +
                "        lineitem l,\n" +
                "        orders o,\n" +
                "        customer c,\n" +
                "        nation n1,\n" +
                "        nation n2,\n" +
                "        region r\n" +
                "    where\n" +
                "        p.p_partkey = l.l_partkey\n" +
                "        and s.s_suppkey = l.l_suppkey\n" +
                "        and l.l_orderkey = o.o_orderkey\n" +
                "        and o.o_custkey = c.c_custkey\n" +
                "        and c.c_nationkey = n1.n_nationkey\n" +
                "        and n1.n_regionkey = r.r_regionkey\n" +
                "        and r.r_name = '[REGION]'\n" +
                "        and s.s_nationkey = n2.n_nationkey\n" +
                "        and o.o_orderdate between date '1995-01-01' and date '1996-12-31'\n" +
                "        and p.p_type = '[TYPE]'\n" +
                "    ) as all_nations\n" +
                "group by\n" +
                "    o_year\n" +
                "order by\n" +
                "    o_year"
        );

        return;
    }

    // TODO: Grouping set assertion
    @Test
    public void testQ9() {
        RelNode rootInput = optimizePhysical(
            "select\n" +
                "    nation,\n" +
                "    o_year,\n" +
                "    sum(amount) as sum_profit\n" +
                "from (\n" +
                "    select\n" +
                "        n.n_name as nation,\n" +
                "        extract(year from o.o_orderdate) as o_year,\n" +
                "        l.l_extendedprice * (1 - l.l_discount) - ps.ps_supplycost * l.l_quantity as amount\n" +
                "    from\n" +
                "        part p,\n" +
                "        supplier s,\n" +
                "        lineitem l,\n" +
                "        partsupp ps,\n" +
                "        orders o,\n" +
                "        nation n\n" +
                "    where\n" +
                "        s.s_suppkey = l.l_suppkey\n" +
                "        and ps.ps_suppkey = l.l_suppkey\n" +
                "        and ps.ps_partkey = l.l_partkey\n" +
                "        and p.p_partkey = l.l_partkey\n" +
                "        and o.o_orderkey = l.l_orderkey\n" +
                "        and s.s_nationkey = n.n_nationkey\n" +
                "        and p.p_name like '%[COLOR]%'\n" +
                "    ) as profit\n" +
                "group by\n" +
                "    nation,\n" +
                "    o_year\n" +
                "order by\n" +
                "    nation,\n" +
                "    o_year desc"
        );

        return;
    }

    // TODO: Agg grouping sets
    @Test
    public void testQ10() {
        RelNode rootInput = optimizePhysical(
            "select\n" +
                "    c.c_custkey,\n" +
                "    c.c_name,\n" +
                "    sum(l.l_extendedprice * (1 - l.l_discount)) as revenue,\n" +
                "    c.c_acctbal,\n" +
                "    n.n_name,\n" +
                "    c.c_address,\n" +
                "    c.c_phone,\n" +
                "    c.c_comment\n" +
                "from\n" +
                "    customer c,\n" +
                "    orders o,\n" +
                "    lineitem l,\n" +
                "    nation n\n" +
                "where\n" +
                "    c.c_custkey = o.o_custkey\n" +
                "    and l.l_orderkey = o.o_orderkey\n" +
                "    and o.o_orderdate >= date '1998-12-01'\n" +
                "    and o.o_orderdate < date '1999-03-01'\n" +
//                "    and o.o_orderdate >= date '[DATE]'\n" + // TODO
//                "    and o.o_orderdate < date '[DATE]' + interval '3' month\n" +
                "    and l.l_returnflag = 'R'\n" +
                "    and c.c_nationkey = n.n_nationkey\n" +
                "group by\n" +
                "    c.c_custkey,\n" +
                "    c.c_name,\n" +
                "    c.c_acctbal,\n" +
                "    c.c_phone,\n" +
                "    n.n_name,\n" +
                "    c.c_address,\n" +
                "    c.c_comment\n" +
                "order by\n" +
                "    revenue desc"
        );

        return;
    }

    // TODO: Hash agg
    @Test
    public void testQ11() {
        RelNode rootInput = optimizePhysical(
            "select\n" +
                "    ps.ps_partkey,\n" +
                "    sum(ps.ps_supplycost * ps.ps_availqty) as val\n" +
                "from\n" +
                "    partsupp ps,\n" +
                "    supplier s,\n" +
                "    nation n\n" +
                "where\n" +
                "    ps.ps_suppkey = s.s_suppkey\n" +
                "    and s.s_nationkey = n.n_nationkey\n" +
                "    and n.n_name = '[NATION]'\n" +
                "group by\n" +
                "    ps.ps_partkey having\n" +
                "        sum(ps.ps_supplycost * ps.ps_availqty) > (\n" +
                "            select\n" +
                "                sum(ps2.ps_supplycost * ps2.ps_availqty) * 0.9\n" +
//                "                sum(ps2.ps_supplycost * ps2.ps_availqty) * [FRACTION]\n" +
                "            from\n" +
                "                partsupp ps2,\n" +
                "                supplier s2,\n" +
                "                nation n2\n" +
                "            where\n" +
                "                ps2.ps_suppkey = s2.s_suppkey\n" +
                "                and s2.s_nationkey = n2.n_nationkey\n" +
                "                and n2.n_name = '[NATION]'\n" +
                "        )\n" +
                "order by\n" +
                "    val desc"
        );

        return;
    }

    // TODO: Hash agg
    @Test
    public void testQ12() {
        RelNode rootInput = optimizePhysical(
            "select\n" +
                "    l.l_shipmode,\n" +
                "    sum(case\n" +
                "        when o.o_orderpriority ='1-URGENT' or o.o_orderpriority ='2-HIGH'\n" +
                "        then 1\n" +
                "        else 0\n" +
                "    end) as high_line_count,\n" +
                "    sum(case\n" +
                "        when o.o_orderpriority <> '1-URGENT' and o.o_orderpriority <> '2-HIGH'\n" +
                "        then 1\n" +
                "        else 0\n" +
                "    end) as low_line_count\n" +
                "from\n" +
                "    orders o,\n" +
                "    lineitem l\n" +
                "where\n" +
                "    o.o_orderkey = l.l_orderkey\n" +
                "    and l.l_shipmode in ('[SHIPMODE1]', '[SHIPMODE2]')\n" +
                "    and l.l_commitdate < l.l_receiptdate\n" +
                "    and l.l_shipdate < l.l_commitdate\n" +
                "    and l.l_receiptdate >= date '1998-12-01'\n" +
                "    and l.l_receiptdate < date '1999-12-01'\n" +
//                "    and l.l_receiptdate >= date '[DATE]'\n" + // TODO
//                "    and l.l_receiptdate < date '[DATE]' + interval '1' year\n" +
                "group by\n" +
                "    l.l_shipmode\n" +
                "order by\n" +
                "    l.l_shipmode"
        );

        return;
    }

    // TODO: Hash agg
    @Test
    public void testQ13() {
        RelNode rootInput = optimizePhysical(
            "select\n" +
                "    c_count, count(*) as custdist\n" +
                "from (\n" +
                "    select\n" +
                "        c.c_custkey,\n" +
                "        count(o.o_orderkey)\n" +
                "    from\n" +
                "        customer c left outer join orders o on\n" +
                "            c.c_custkey = o.o_custkey\n" +
                "            and o.o_comment not like '%[WORD1]%[WORD2]%'\n" +
                "    group by\n" +
                "        c.c_custkey\n" +
                "    )as c_orders (c_custkey, c_count)\n" +
                "group by\n" +
                "    c_count\n" +
                "order by\n" +
                "    custdist desc,\n" +
                "    c_count desc"
        );

        return;
    }

    // TODO: Hash agg
    @Test
    public void testQ14() {
        RelNode rootInput = optimizePhysical(
            "select\n" +
                "    100.00 * sum(case\n" +
                "        when p.p_type like 'PROMO%'\n" +
                "        then l.l_extendedprice*(1 - l.l_discount)\n" +
                "        else 0\n" +
                "    end) / sum(l.l_extendedprice * (1 - l.l_discount)) as promo_revenue\n" +
                "from\n" +
                "    lineitem l,\n" +
                "    part p\n" +
                "where\n" +
                "    l.l_partkey = p.p_partkey\n" +
                "    and l.l_shipdate >= date '1998-12-01'\n" +
                "    and l.l_shipdate < date '1999-01-01'"
//                "    and l.l_shipdate >= date '[DATE]'\n" + // TODO
//                "    and l.l_shipdate < date '[DATE]' + interval '1' month;"
        );

        return;
    }

    // TODO: Views!
    @Test
    public void testQ15() {
        fail("We need views or table expressions to support this query");
    }

    // TODO: Grouping set assertion
    // TODO: COUNT DISTINCT
    @Test
    public void testQ16() {
        RelNode rootInput = optimizePhysical(
            "select\n" +
                "    p.p_brand,\n" +
                "    p.p_type,\n" +
                "    p.p_size,\n" +
                "    count(distinct ps.ps_suppkey) as supplier_cnt\n" +
                "from\n" +
                "    partsupp ps,\n" +
                "    part p\n" +
                "where\n" +
                "    p.p_partkey = ps.ps_partkey\n" +
                "    and p.p_brand <> '[BRAND]'\n" +
                "    and p.p_type not like '[TYPE]%'\n" +
                "    and p.p_size in (1, 2, 3, 4, 5, 6, 7, 8)\n" +
//                "    and p_size in ([SIZE1], [SIZE2], [SIZE3], [SIZE4], [SIZE5], [SIZE6], [SIZE7], [SIZE8])\n" + // TODO
                "    and ps.ps_suppkey not in (\n" +
                "        select\n" +
                "            s_suppkey\n" +
                "        from\n" +
                "            supplier\n" +
                "        where\n" +
                "            s_comment like '%Customer%Complaints%'\n" +
                "    )\n" +
                "group by\n" +
                "    p.p_brand,\n" +
                "    p.p_type,\n" +
                "    p.p_size\n" +
                "order by\n" +
                "    supplier_cnt desc,\n" +
                "    p.p_brand,\n" +
                "    p.p_type,\n" +
                "    p.p_size"
        );

        return;
    }

    // TODO: Hash agg
    @Test
    public void testQ17() {
        RelNode rootInput = optimizePhysical(
            "select\n" +
                "    sum(l.l_extendedprice) / 7.0 as avg_yearly\n" +
                "from\n" +
                "    lineitem l,\n" +
                "    part p\n" +
                "where\n" +
                "    p.p_partkey = l.l_partkey\n" +
                "    and p.p_brand = '[BRAND]'\n" +
                "    and p.p_container = '[CONTAINER]'\n" +
                "    and l.l_quantity < (\n" +
                "        select\n" +
                "            0.2 * avg(l2.l_quantity)\n" +
                "        from\n" +
                "            lineitem l2\n" +
                "        where\n" +
                "            l2.l_partkey = p.p_partkey\n" +
                ")"
        );

        return;
    }

    // TODO: Grouping set assert
    @Test
    public void testQ18() {
        RelNode rootInput = optimizePhysical(
            "select\n" +
                "    c.c_name,\n" +
                "    c.c_custkey,\n" +
                "    o.o_orderkey,\n" +
                "    o.o_orderdate,\n" +
                "    o.o_totalprice,\n" +
                "    sum(l.l_quantity)\n" +
                "from\n" +
                "    customer c,\n" +
                "    orders o,\n" +
                "    lineitem l\n" +
                "where\n" +
                "    o.o_orderkey in (\n" +
                "        select\n" +
                "            l2.l_orderkey\n" +
                "        from\n" +
                "            lineitem l2\n" +
                "        group by\n" +
                "            l2.l_orderkey having\n" +
                "                sum(l2.l_quantity) > 100\n" +
//                "                sum(l2.l_quantity) > [QUANTITY]\n" + // TODO
                "    )\n" +
                "    and c.c_custkey = o.o_custkey\n" +
                "    and o.o_orderkey = l.l_orderkey\n" +
                "group by\n" +
                "    c.c_name,\n" +
                "    c.c_custkey,\n" +
                "    o.o_orderkey,\n" +
                "    o.o_orderdate,\n" +
                "    o.o_totalprice\n" +
                "order by\n" +
                "    o.o_totalprice desc,\n" +
                "    o.o_orderdate"
        );

        return;
    }

    // TODO: Hash agg
    @Test
    public void testQ19() {
        RelNode rootInput = optimizePhysical(
            "select\n" +
                "    sum(l.l_extendedprice * (1 - l.l_discount) ) as revenue\n" +
                "from\n" +
                "    lineitem l,\n" +
                "    part p\n" +
                "where\n" +
                "    (\n" +
                "        p.p_partkey = l.l_partkey\n" +
                "        and p.p_brand = '[BRAND1]'\n" +
                "        and p.p_container in ( 'SM CASE', 'SM BOX', 'SM PACK', 'SM PKG')\n" +
                "        and l.l_quantity >= 10 and l.l_quantity <= 10 + 10\n" +
//                "        and l.l_quantity >= [QUANTITY1] and l.l_quantity <= [QUANTITY1] + 10\n" +// TODO
                "        and p.p_size between 1 and 5\n" +
                "        and l.l_shipmode in ('AIR', 'AIR REG')\n" +
                "        and l.l_shipinstruct = 'DELIVER IN PERSON'\n" +
                "    )\n" +
                "    or\n" +
                "    (\n" +
                "        p.p_partkey = l.l_partkey\n" +
                "        and p.p_brand = '[BRAND2]'\n" +
                "        and p.p_container in ('MED BAG', 'MED BOX', 'MED PKG', 'MED PACK')\n" +
                "        and l.l_quantity >= 20 and l.l_quantity <= 20 + 10\n" +
//                "        and l.l_quantity >= [QUANTITY2] and l.l_quantity <= [QUANTITY2] + 10\n" + // TODO
                "        and p.p_size between 1 and 10\n" +
                "        and l.l_shipmode in ('AIR', 'AIR REG')\n" +
                "        and l.l_shipinstruct = 'DELIVER IN PERSON'\n" +
                "    )\n" +
                "    or\n" +
                "    (\n" +
                "        p.p_partkey = l.l_partkey\n" +
                "        and p.p_brand = '[BRAND3]'\n" +
                "        and p.p_container in ( 'LG CASE', 'LG BOX', 'LG PACK', 'LG PKG')\n" +
                "        and l.l_quantity >= 30 and l.l_quantity <= 30 + 10\n" +
//                "        and l.l_quantity >= [QUANTITY3] and l.l_quantity <= [QUANTITY3] + 10\n" + // TODO
                "        and p.p_size between 1 and 15\n" +
                "        and l.l_shipmode in ('AIR', 'AIR REG')\n" +
                "        and l.l_shipinstruct = 'DELIVER IN PERSON'\n" +
                "    )"
        );

        return;
    }

    // TODO: Grouping set assertion
    @Test
    public void testQ20() {
        RelNode rootInput = optimizePhysical(
            "select\n" +
                "    s.s_name,\n" +
                "    s.s_address\n" +
                "from\n" +
                "    supplier s, \n" +
                "    nation n\n" +
                "where\n" +
                "    s.s_suppkey in (\n" +
                "        select\n" +
                "            ps.ps_suppkey\n" +
                "        from\n" +
                "            partsupp ps\n" +
                "        where\n" +
                "            ps.ps_partkey in (\n" +
                "                select\n" +
                "                    p.p_partkey\n" +
                "                from\n" +
                "                    part p\n" +
                "                where\n" +
                "                    p.p_name like '[COLOR]%'\n" +
                "            )\n" +
                "            and ps.ps_availqty > (\n" +
                "                select\n" +
                "                    0.5 * sum(l.l_quantity)\n" +
                "                from\n" +
                "                    lineitem l\n" +
                "                where\n" +
                "                    l.l_partkey = ps.ps_partkey\n" +
                "                    and l.l_suppkey = ps.ps_suppkey\n" +
                "                    and l.l_shipdate >= date '1998-12-01'\n" +
                "                    and l.l_shipdate < date '1999-12-01'\n" +
//                "                    and l.l_shipdate >= date('[DATE]')\n" + // TODO
//                "                    and l.l_shipdate < date('[DATE]') + interval ‘1' year\n" +
                "            )\n" +
                "    )\n" +
                "    and s.s_nationkey = n.n_nationkey\n" +
                "    and n.n_name = '[NATION]'\n" +
                "order by\n" +
                "    s.s_name"
        );

        return;
    }

    // TODO: Grouping set assertion
    @Test
    public void testQ21() {
        RelNode rootInput = optimizePhysical(
            "select\n" +
                "    s.s_name,\n" +
                "    count(*) as numwait\n" +
                "from\n" +
                "    supplier s,\n" +
                "    lineitem l1,\n" +
                "    orders o,\n" +
                "    nation n\n" +
                "where\n" +
                "    s._suppkey = l1.l_suppkey\n" +
                "    and o.o_orderkey = l1.l_orderkey\n" +
                "    and o.o_orderstatus = 'F'\n" +
                "    and l1.l_receiptdate > l1.l_commitdate\n" +
                "    and exists (\n" +
                "        select\n" +
                "            *\n" +
                "        from\n" +
                "            lineitem l2\n" +
                "        where\n" +
                "            l2.l_orderkey = l1.l_orderkey\n" +
                "            and l2.l_suppkey <> l1.l_suppkey\n" +
                "    )\n" +
                "    and not exists (\n" +
                "        select\n" +
                "            *\n" +
                "\t\tfrom\n" +
                "            lineitem l3\n" +
                "        where\n" +
                "            l3.l_orderkey = l1.l_orderkey\n" +
                "            and l3.l_suppkey <> l1.l_suppkey\n" +
                "            and l3.l_receiptdate > l3.l_commitdate\n" +
                "    )\n" +
                "    and s.s_nationkey = n.n_nationkey\n" +
                "    and n.n_name = '[NATION]'\n" +
                "group by\n" +
                "    s.s_name\n" +
                "order by\n" +
                "    numwait desc,\n" +
                "    s.s_name"
        );

        return;
    }

    // TODO: Hash agg
    @Test
    public void testQ22() {
        RelNode rootInput = optimizePhysical(
            "select\n" +
                "    cntrycode,\n" +
                "    count(*) as numcust,\n" +
                "    sum(c_acctbal) as totacctbal\n" +
                "from (\n" +
                "    select\n" +
                "        substring(c.c_phone from 1 for 2) as cntrycode,\n" +
                "        c.c_acctbal\n" +
                "    from\n" +
                "        customer c\n" +
                "    where\n" +
                "        substring(c.c_phone from 1 for 2) in ('[I1]','[I2]','[I3]','[I4]','[I5]','[I6]','[I7]')\n" +
                "        and c.c_acctbal > (\n" +
                "            select\n" +
                "                avg(c2.c_acctbal)\n" +
                "            from\n" +
                "                customer c2\n" +
                "            where\n" +
                "                c2.c_acctbal > 0.00\n" +
                "                and substring (c2.c_phone from 1 for 2) in ('[I1]','[I2]','[I3]','[I4]','[I5]','[I6]','[I7]')\n" +
                "        )\n" +
                "        and not exists (\n" +
                "            select\n" +
                "                *\n" +
                "            from\n" +
                "                orders o\n" +
                "            where\n" +
                "                o.o_custkey = c.c_custkey\n" +
                "        )\n" +
                "    ) as custsale\n" +
                "group by\n" +
                "    cntrycode\n" +
                "order by\n" +
                "    cntrycode"
        );

        return;
    }
}
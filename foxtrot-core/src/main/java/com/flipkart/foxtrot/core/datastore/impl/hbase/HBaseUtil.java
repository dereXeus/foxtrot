/**
 * Copyright 2014 Flipkart Internet Pvt. Ltd.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flipkart.foxtrot.core.datastore.impl.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.io.hfile.Compression;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public abstract class HBaseUtil {
    private static final Logger logger = LoggerFactory.getLogger(HBaseUtil.class);

    public static Configuration create(final HbaseConfig hbaseConfig) throws IOException {
        Configuration configuration = HBaseConfiguration.create();
        if(hbaseConfig.isSecure()
                && null != hbaseConfig.getKeytabFileName()
                && !hbaseConfig.getKeytabFileName().isEmpty()) {
            File file = new File(hbaseConfig.getKeytabFileName());
            if (file.exists()) {
                configuration.addResource(new File(hbaseConfig.getCoreSite()).toURI().toURL());
                configuration.addResource(new File(hbaseConfig.getHdfsSite()).toURI().toURL());
                configuration.addResource(new File(hbaseConfig.getHbasePolicy()).toURI().toURL());
                configuration.addResource(new File(hbaseConfig.getHbaseSite()).toURI().toURL());
                configuration.set("hbase.master.kerberos.principal", hbaseConfig.getAuthString());
                configuration.set("hadoop.kerberos.kinit.command", hbaseConfig.getKinitPath());
                UserGroupInformation.setConfiguration(configuration);
                System.setProperty("java.security.krb5.conf", hbaseConfig.getKerberosConfigFile());
                if (hbaseConfig.isSecure()) {
                    UserGroupInformation.loginUserFromKeytab(
                            hbaseConfig.getAuthString(), hbaseConfig.getKeytabFileName());
                    logger.info("Logged into Hbase with User: " + UserGroupInformation.getLoginUser());
                }
            }
        }
        return configuration;
    }

    public static void createTable(final HbaseConfig hbaseConfig, final String tableName) throws IOException {
        HBaseAdmin hBaseAdmin = new HBaseAdmin(HBaseUtil.create(hbaseConfig));
        HTableDescriptor hTableDescriptor = new HTableDescriptor(tableName);
        HColumnDescriptor columnDescriptor = new HColumnDescriptor("d");
        columnDescriptor.setCompressionType(Compression.Algorithm.GZ);
        hTableDescriptor.addFamily(columnDescriptor);
        try {
            hBaseAdmin.createTable(hTableDescriptor);
        } catch (Exception e) {
          logger.error("Could not create table: " + tableName, e);
        } finally {
            hBaseAdmin.close();
        }
    }
}
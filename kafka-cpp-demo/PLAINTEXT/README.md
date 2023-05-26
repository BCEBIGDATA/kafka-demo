# VPC网络PLAINTEXT方式生产和消费
在同 VPC 网络下访问，使用 PLAINTEXT 协议接入，接入点可以在 【集群详情】 页面查看。
具体请参考接入点查看
## 环境准备
1. [安装GCC](https://gcc.gnu.org/install/?spm=a2c4g.159954.0.0.4f6744d285uMBi)
2. 安装C++ 依赖库。
```shell
   yum install librdkafka-devel
   yum install cyrus-sasl
   yum install cyrus-sasl-scram
```
## 集群准备
### 1. 购买专享版消息服务for Kafka集群
开通消息服务 for Kafka服务后，在控制台页面点击『创建集群』，即可进行购买。
![img.png](../../img/img.png)
### 2. 为购买的集群创建主题
在控制台页面点击集群名称，进入集群详情页面。
在左侧的边栏中点击『主题管理』，进入主题管理页面。
![img.png](../../img/img1.png)
在主题管理页面点击『创建主题』，进行主题的创建。
## 使用步骤
### 步骤一：获取集群接入点
具体请参考：[接入点查看]()。
### 步骤二：编写测试代码
#### 生产者代码示例
创建KafkaProducerDemo.c文件，具体代码示例如下：
```cpp
/*
* librdkafka - Apache Kafka C library
*
* Copyright (c) 2017, Magnus Edenhill
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*
* 1. Redistributions of source code must retain the above copyright notice,
*    this list of conditions and the following disclaimer.
* 2. Redistributions in binary form must reproduce the above copyright notice,
*    this list of conditions and the following disclaimer in the documentation
*    and/or other materials provided with the distribution.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
* POSSIBILITY OF SUCH DAMAGE.
  */

/**
* Simple Apache Kafka producer
* using the Kafka driver from librdkafka
* (https://github.com/edenhill/librdkafka)
  */

#include <stdio.h>
#include <signal.h>
#include <string.h>


/* Typical include path would be <librdkafka/rdkafka.h>, but this program
* is builtin from within the librdkafka source tree and thus differs. */
  #include "librdkafka/rdkafka.h"


static volatile sig_atomic_t run = 1;

/**
* @brief Signal termination of program
  */
  static void stop(int sig) {
  run = 0;
  fclose(stdin); /* abort fgets() */
  }


/**
* @brief Message delivery report callback.
*
* This callback is called exactly once per message, indicating if
* the message was succesfully delivered
* (rkmessage->err == RD_KAFKA_RESP_ERR_NO_ERROR) or permanently
* failed delivery (rkmessage->err != RD_KAFKA_RESP_ERR_NO_ERROR).
*
* The callback is triggered from rd_kafka_poll() and executes on
* the application's thread.
  */
  static void dr_msg_cb(rd_kafka_t *rk, const rd_kafka_message_t *rkmessage, void *opaque) {
  if (rkmessage->err) {
  fprintf(stderr, "%% Message delivery failed: %s\n", rd_kafka_err2str(rkmessage->err));
  } else {
  fprintf(stderr, "%% Message delivered (%zd bytes, partition %" PRId32 ")\n", rkmessage->len, rkmessage->partition);
  }

  /* The rkmessage is destroyed automatically by librdkafka */
  }



int main(int argc, char **argv) {
rd_kafka_t *rk;        /* Producer instance handle */
rd_kafka_conf_t *conf; /* Temporary configuration object */

    char errstr[512];      /* librdkafka API error reporting buffer */
    char buf[512];         /* Message value temporary buffer */

    const char *brokers;   /* Argument: broker list */
    const char *topic;     /* Argument: topic to produce to */
    // const char *username;      /* Argument: sasl username */
    // const char *password;      /* Argument: sasl password */

    /*
     * Argument validation
     */
    if (argc != 3) {
        fprintf(stderr, "%% Usage: %s <broker> <topic>\n", argv[0]);
        return 1;
    }

    brokers = argv[1];
    topic   = argv[2];
    // username = argv[3];
    // password = argv[4];


    /*
     * Create Kafka client configuration place-holder
     */
    conf = rd_kafka_conf_new();

    /* Set bootstrap broker(s) as a comma-separated list of
     * host or host:port (default port 9092).
     * librdkafka will use the bootstrap brokers to acquire the full
     * set of brokers from the cluster. */
    if (rd_kafka_conf_set(conf, "bootstrap.servers", brokers, errstr, sizeof(errstr)) != RD_KAFKA_CONF_OK) {
        fprintf(stderr, "%s\n", errstr);
        return 1;
    }

    if (
            rd_kafka_conf_set(conf, "security.protocol", "plaintext", errstr, sizeof(errstr)) != RD_KAFKA_CONF_OK
    ) {
        fprintf(stderr, "%s\n", errstr);
        return -1;
    }

    /* Set the delivery report callback.
     * This callback will be called once per message to inform
     * the application if delivery succeeded or failed.
     * See dr_msg_cb() above.
     * The callback is only triggered from rd_kafka_poll() and
     * rd_kafka_flush(). */
    rd_kafka_conf_set_dr_msg_cb(conf, dr_msg_cb);

    /*
     * Create producer instance.
     *
     * NOTE: rd_kafka_new() takes ownership of the conf object
     *       and the application must not reference it again after
     *       this call.
     */
    rk = rd_kafka_new(RD_KAFKA_PRODUCER, conf, errstr, sizeof(errstr));
    if (!rk) {
        fprintf(stderr, "%% Failed to create new producer: %s\n", errstr);
        return 1;
    }

    /* Signal handler for clean shutdown */
    signal(SIGINT, stop);

    fprintf(stderr,
            "%% Type some text and hit enter to produce message\n"
            "%% Or just hit enter to only serve delivery reports\n"
            "%% Press Ctrl-C or Ctrl-D to exit\n");

    while (run && fgets(buf, sizeof(buf), stdin)) {
        size_t len = strlen(buf);
        rd_kafka_resp_err_t err;

        /* Remove newline */
        if (buf[len - 1] == '\n') {
            buf[--len] = '\0';
        }

        /* Empty line: only serve delivery reports */
        if (len == 0) {
            rd_kafka_poll(rk, 0/*non-blocking */);
            continue;
        }

        /*
         * Send/Produce message.
         * This is an asynchronous call, on success it will only
         * enqueue the message on the internal producer queue.
         * The actual delivery attempts to the broker are handled
         * by background threads.
         * The previously registered delivery report callback
         * (dr_msg_cb) is used to signal back to the application
         * when the message has been delivered (or failed).
         */
    retry:
        err = rd_kafka_producev(
            /* Producer handle */
            rk,
            /* Topic name */
            RD_KAFKA_V_TOPIC(topic),
            /* Make a copy of the payload. */
            RD_KAFKA_V_MSGFLAGS(RD_KAFKA_MSG_F_COPY),
            /* Message value and length */
            RD_KAFKA_V_VALUE(buf, len),
            /* Per-Message opaque, provided in
             * delivery report callback as
             * msg_opaque. */
            RD_KAFKA_V_OPAQUE(NULL),
            /* End sentinel */
            RD_KAFKA_V_END
        );

        if (err) {
            /*
             * Failed to *enqueue* message for producing.
             */
            fprintf(stderr,
                    "%% Failed to produce to topic %s: %s\n", topic,
                    rd_kafka_err2str(err));

            if (err == RD_KAFKA_RESP_ERR__QUEUE_FULL) {
                /* If the internal queue is full, wait for
                 * messages to be delivered and then retry.
                 * The internal queue represents both
                 * messages to be sent and messages that have
                 * been sent or failed, awaiting their
                 * delivery report callback to be called.
                 *
                 * The internal queue is limited by the
                 * configuration property
                 * queue.buffering.max.messages */
                rd_kafka_poll(rk, 1000 /*block for max 1000ms*/);
                goto retry;
            }
        } else {
            fprintf(stderr, "%% Enqueued message (%zd bytes) "
                    "for topic %s\n",
                    len, topic);
        }


        /* A producer application should continually serve
         * the delivery report queue by calling rd_kafka_poll()
         * at frequent intervals.
         * Either put the poll call in your main loop, or in a
         * dedicated thread, or call it after every
         * rd_kafka_produce() call.
         * Just make sure that rd_kafka_poll() is still called
         * during periods where you are not producing any messages
         * to make sure previously produced messages have their
         * delivery report callback served (and any other callbacks
         * you register). */
        rd_kafka_poll(rk, 0 /*non-blocking*/);
    }


    /* Wait for final messages to be delivered or fail.
     * rd_kafka_flush() is an abstraction over rd_kafka_poll() which
     * waits for all messages to be delivered. */
    fprintf(stderr, "%% Flushing final messages..\n");
    rd_kafka_flush(rk, 10 * 1000 /* wait for max 10 seconds */);

    /* If the output queue is still not empty there is an issue
     * with producing messages to the clusters. */
    if (rd_kafka_outq_len(rk) > 0) {
        fprintf(stderr, "%% %d message(s) were not delivered\n", rd_kafka_outq_len(rk));
    }

    /* Destroy the producer instance */
    rd_kafka_destroy(rk);

    return 0;
}
```
#### 消费者代码示例
创建KafkaConsumerDemo.c文件，具体代码示例如下：
```cpp
/*
* librdkafka - Apache Kafka C library
*
* Copyright (c) 2019, Magnus Edenhill
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*
* 1. Redistributions of source code must retain the above copyright notice,
*    this list of conditions and the following disclaimer.
* 2. Redistributions in binary form must reproduce the above copyright notice,
*    this list of conditions and the following disclaimer in the documentation
*    and/or other materials provided with the distribution.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
* POSSIBILITY OF SUCH DAMAGE.
  */

/**
* Simple high-level balanced Apache Kafka consumer
* using the Kafka driver from librdkafka
* (https://github.com/edenhill/librdkafka)
  */

#include <stdio.h>
#include <signal.h>
#include <string.h>
#include <ctype.h>


/* Typical include path would be <librdkafka/rdkafka.h>, but this program
* is builtin from within the librdkafka source tree and thus differs. */
  //#include <librdkafka/rdkafka.h>
  #include "librdkafka/rdkafka.h"


static volatile sig_atomic_t run = 1;

/**
* @brief Signal termination of program
  */
  static void stop(int sig) {
  run = 0;
  }



/**
* @returns 1 if all bytes are printable, else 0.
  */
  static int is_printable(const char *buf, size_t size) {
  size_t i;

  for (i = 0; i < size; i++) {
  if (!isprint((int)buf[i])) {
  return 0;
  }
  }

  return 1;
  }


int main(int argc, char **argv) {
rd_kafka_t *rk;          /* Consumer instance handle */
rd_kafka_conf_t *conf;   /* Temporary configuration object */
rd_kafka_resp_err_t err; /* librdkafka API error code */

    char errstr[512];        /* librdkafka API error reporting buffer */

    const char *brokers;     /* Argument: broker list */
    const char *groupid;     /* Argument: Consumer group id */
    char **topics;           /* Argument: list of topics to subscribe to */

    int topic_cnt;           /* Number of topics to subscribe to */
    rd_kafka_topic_partition_list_t *subscription; /* Subscribed topics */
    int i;

    /*
     * Argument validation
     */
    if (argc < 4) {
        fprintf(stderr, "%% Usage: %s <broker> <group.id> <username> <password> <topic1> <topic2>..\n", argv[0]);
        return 1;
    }

    brokers   = argv[1];
    groupid   = argv[2];
    topics    = &argv[3];

    topic_cnt = argc - 3;


    /*
     * Create Kafka client configuration place-holder
     */
    conf = rd_kafka_conf_new();

    /* Set bootstrap broker(s) as a comma-separated list of
     * host or host:port (default port 9092).
     * librdkafka will use the bootstrap brokers to acquire the full
     * set of brokers from the cluster. */
    if (rd_kafka_conf_set(conf, "bootstrap.servers", brokers, errstr, sizeof(errstr)) != RD_KAFKA_CONF_OK) {
        fprintf(stderr, "%s\n", errstr);
        rd_kafka_conf_destroy(conf);
        return 1;
    }

    if (
            rd_kafka_conf_set(conf, "security.protocol", "plaintext", errstr, sizeof(errstr)) != RD_KAFKA_CONF_OK
    ) {
        fprintf(stderr, "%s\n", errstr);
        return -1;
    }

    /* Set the consumer group id.
     * All consumers sharing the same group id will join the same
     * group, and the subscribed topic' partitions will be assigned
     * according to the partition.assignment.strategy
     * (consumer config property) to the consumers in the group. */
    if (rd_kafka_conf_set(conf, "group.id", groupid, errstr, sizeof(errstr)) != RD_KAFKA_CONF_OK) {
        fprintf(stderr, "%s\n", errstr);
        rd_kafka_conf_destroy(conf);
        return 1;
    }

    /* If there is no previously committed offset for a partition
     * the auto.offset.reset strategy will be used to decide where
     * in the partition to start fetching messages.
     * By setting this to earliest the consumer will read all messages
     * in the partition if there was no previously committed offset. */
    if (rd_kafka_conf_set(conf, "auto.offset.reset", "earliest", errstr, sizeof(errstr)) != RD_KAFKA_CONF_OK) {
        fprintf(stderr, "%s\n", errstr);
        rd_kafka_conf_destroy(conf);
        return 1;
    }

    /*
     * Create consumer instance.
     *
     * NOTE: rd_kafka_new() takes ownership of the conf object
     *       and the application must not reference it again after
     *       this call.
     */
    rk = rd_kafka_new(RD_KAFKA_CONSUMER, conf, errstr, sizeof(errstr));
    if (!rk) {
        fprintf(stderr, "%% Failed to create new consumer: %s\n", errstr);
        return 1;
    }

    conf = NULL; /* Configuration object is now owned, and freed,
                  * by the rd_kafka_t instance. */


    /* Redirect all messages from per-partition queues to
     * the main queue so that messages can be consumed with one
     * call from all assigned partitions.
     *
     * The alternative is to poll the main queue (for events)
     * and each partition queue separately, which requires setting
     * up a rebalance callback and keeping track of the assignment:
     * but that is more complex and typically not recommended. */
    rd_kafka_poll_set_consumer(rk);


    /* Convert the list of topics to a format suitable for librdkafka */
    subscription = rd_kafka_topic_partition_list_new(topic_cnt);
    for (i = 0; i < topic_cnt; i++) {
        rd_kafka_topic_partition_list_add(
            subscription,
            topics[i],
            /* the partition is ignored
             * by subscribe() */
            RD_KAFKA_PARTITION_UA
        );
    }

    /* Subscribe to the list of topics */
    err = rd_kafka_subscribe(rk, subscription);
    if (err) {
        fprintf(stderr, "%% Failed to subscribe to %d topics: %s\n", subscription->cnt, rd_kafka_err2str(err));
        rd_kafka_topic_partition_list_destroy(subscription);
        rd_kafka_destroy(rk);
        return 1;
    }

    fprintf(stderr,
            "%% Subscribed to %d topic(s), "
            "waiting for rebalance and messages...\n",
            subscription->cnt);

    rd_kafka_topic_partition_list_destroy(subscription);


    /* Signal handler for clean shutdown */
    signal(SIGINT, stop);

    /* Subscribing to topics will trigger a group rebalance
     * which may take some time to finish, but there is no need
     * for the application to handle this idle period in a special way
     * since a rebalance may happen at any time.
     * Start polling for messages. */

    while (run) {
        rd_kafka_message_t *rkm;

        rkm = rd_kafka_consumer_poll(rk, 100);
        if (!rkm) {
            continue; /* Timeout: no message within 100ms,
                       *  try again. This short timeout allows
                       *  checking for `run` at frequent intervals.
                       */
        }

        /* consumer_poll() will return either a proper message
         * or a consumer error (rkm->err is set). */
        if (rkm->err) {
            /* Consumer errors are generally to be considered
             * informational as the consumer will automatically
             * try to recover from all types of errors. */
            fprintf(stderr, "%% Consumer error: %s\n", rd_kafka_message_errstr(rkm));
            rd_kafka_message_destroy(rkm);
            continue;
        }

        /* Proper message. */
        printf("Message on %s [%" PRId32 "] at offset %" PRId64 ":\n",
               rd_kafka_topic_name(rkm->rkt), rkm->partition,
               rkm->offset);

        /* Print the message key. */
        if (rkm->key && is_printable(rkm->key, rkm->key_len)) {
            printf(" Key: %.*s\n", (int)rkm->key_len, (const char *)rkm->key);
        } else if (rkm->key) {
            printf(" Key: (%d bytes)\n", (int)rkm->key_len);
        }

        /* Print the message value/payload. */
        if (rkm->payload && is_printable(rkm->payload, rkm->len)) {
            printf(" Value: %.*s\n", (int)rkm->len, (const char *)rkm->payload);
        } else if (rkm->payload) {
            printf(" Value: (%d bytes)\n", (int)rkm->len);
        }

        rd_kafka_message_destroy(rkm);
    }


    /* Close the consumer: commit final offsets and leave the group. */
    fprintf(stderr, "%% Closing consumer\n");
    rd_kafka_consumer_close(rk);


    /* Destroy the consumer */
    rd_kafka_destroy(rk);

    return 0;
}
```
### 步骤三：编译并运行
编译并运行上述两个代码文件。
```shell
## 启动消费者
gcc -lrdkafka ./consumer.c -o consumer
./consumer <broker> <group.id> <topic1> <topic2>..
# 启动生产者
gcc -lrdkafka ./producer.c -o producer
./producer <broker> <topic>
```
### 步骤四：查看集群监控
查看消息是否发送成功或消费成功有两种方式：
1. 在服务器端/控制台查看日志。
2. 在专享版消息服务 for Kafka控制台查看集群监控，获取集群生产、消息情况。

推荐使用第二种方式，下面介绍如何查看集群监控。

（1）在专享版消息服务 for Kafka的控制台页面找到需要连接的集群，点击集群名称进入『集群详情』页面。
![img.png](../../img/img2.png)
（2）页面跳转后，进入左侧边中的『集群详情』页面。
![img.png](../../img/img3.png)
（3）点击左侧边栏中的『集群监控』，进入『集群监控』页面。
![img.png](../../img/img4.png)
（4）通过查看『集群监控』页面，提供的不同纬度的监控信息（集群监控、节点监控、主题监控、消费组监控），即可获知集群的生产和消费情况。
集群监控的具体使用请参考：[集群监控]()
![img.png](../../img/img5.png)
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class KafkaConsumerDemo {
    public static void main(String[] args) throws IOException {

        // 需要自行配置下面三个参数
        // kafka.properties所在路径（建议写文件所在的绝对路径）
        String path = "kafka.properties";
        // 主题名称-topic name
        String topic = "test_sasl_ssl";
        // 消费组id
        String group_id = "test_group";

        // 创建配置类，并获取配置文件 kafka.properties 的内容。
        Properties properties = new Properties();
        File file = new File(path);
        try {
            if (file.exists()) {
                // 如果通过用户指定的path路径找到了kafka.properties文件，则加载kafka.properties中的配置项
                properties.load(new FileInputStream(file));
            } else {
                // 如果没有从path中找到，则从KafkaProducerDemo所在的路径去查找
                properties.load(
                        KafkaProducerDemo.class.getClassLoader().getResourceAsStream("kafka.properties")
                );
            }
        } catch (IOException e) {
            // 没找到kafka.properties文件，在此处处理异常
            throw e;
        }

        // 设置 java.security.auth.login.config，用于加载kafka_client_jaas.conf文件
        if (null == System.getProperty("java.security.auth.login.config")) {
            System.setProperty(
                    "java.security.auth.login.config",
                    properties.getProperty("java.security.auth.login.config")
            );
        }

        // Kafka消息的序列化方式
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        // 指定消费组id
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, group_id);
        // enable.auto.commit如果为true，则消费者的偏移量将定期在后台提交。
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        // 重置消费位点策略:earliest、latest、none
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // 设置kafka自动提交offset的频率，默认5000ms，也就是5s
        properties.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000);
        // 设置消费者在一次poll中返回的最大记录数
        properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1000);
        // 设置消费者两次poll的最大时间间隔
        properties.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 1000);

        // 构建KafkaConsumer对象
        KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(properties);

        //订阅主题
        kafkaConsumer.subscribe(Collections.singleton(topic));

        try{
            //持续消费主题中的消息
            while(true){
                // 构建ConsumerRecord用于接收存储消息
                ConsumerRecords<String, String> kafkaMessage = kafkaConsumer.poll(Duration.ofMillis(5000));
                for (ConsumerRecord<String, String> consumerRecord : kafkaMessage) {
                    // 打印消息具体内容
                    System.out.printf("offset = %d, key = %s, value = %s%n", consumerRecord.offset(), consumerRecord.key(), consumerRecord.value());
                }
            }
        }catch (Exception e){
            System.out.println(e.getMessage());
            e.printStackTrace();
        }finally {
            kafkaConsumer.close();
        }
    }
}
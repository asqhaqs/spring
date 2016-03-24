package cn.javass.spring.chapter9;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import cn.javass.spring.chapter9.model.AddressModel;
import cn.javass.spring.chapter9.model.UserModel;
import cn.javass.spring.chapter9.service.IAddressService;
import cn.javass.spring.chapter9.service.IUserService;
import junit.framework.Assert;

public class TransactionTest {
	
	private static ApplicationContext ctx;
	private static PlatformTransactionManager txManager;
	private static DataSource dataSource;
	private static JdbcTemplate jdbcTemplate;
	
	//id自增主键从0开始
	
	private static final String CREATE_TABLE_SQL = "create table test" +
		    "(id int GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, " +
		    "name varchar(100))";
	 private static final String DROP_TABLE_SQL = "drop table test";

	    
	    private static final String CREATE_USER_TABLE_SQL = "create table user" +
	    "(id int GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, " +
	    "name varchar(100))";

	    private static final String DROP_USER_TABLE_SQL = "drop table user";
	    
	    private static final String CREATE_ADDRESS_TABLE_SQL = "create table address" +
	    "(id int GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, " +
	    "province varchar(100), city varchar(100), street varchar(100), user_id int)";
	    
	    private static final String DROP_ADDRESS_TABLE_SQL = "drop table address";

	    
	    private static final String INSERT_SQL = "insert into test(name) values(?)";
	    private static final String COUNT_SQL = "select count(*) from test";
	    
	    
	    
	    @BeforeClass
	    public static void setUpClass(){
	    	String[] configLocations = new String[]{
	    			"classpath:chapter7/applicationContext-resources.xml",
	                "classpath:chapter9/applicationContext-jdbc.xml"
	    	};
	    	
	    	ctx = new ClassPathXmlApplicationContext(configLocations);
	    	txManager = ctx.getBean(PlatformTransactionManager.class);
	    	dataSource = ctx.getBean(DataSource.class);
	    	jdbcTemplate = new JdbcTemplate(dataSource);
	    }
	    
	    @Test
	    public void testPlatformTransactionManager(){
	    	DefaultTransactionDefinition def = new DefaultTransactionDefinition();
	    	def.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
	    	def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
	    	TransactionStatus status = txManager.getTransaction(def);
	    	jdbcTemplate.execute(CREATE_TABLE_SQL);
	    	try {
				jdbcTemplate.update(INSERT_SQL, "test");
				txManager.commit(status);
			} catch (RuntimeException e) {
				// TODO: handle exception
				txManager.rollback(status);
				
				
			}
	    	jdbcTemplate.execute(DROP_TABLE_SQL);
	    }
	    
	    @Test
	    public void testTransactionTemplate(){
	    	//位于TransactionTest类中
	    	jdbcTemplate.execute(CREATE_TABLE_SQL);
	    	TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
	    	transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
	    	transactionTemplate.execute(new TransactionCallbackWithoutResult() {
				
				
				
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus arg0) {
					// TODO Auto-generated method stub
					jdbcTemplate.update(INSERT_SQL,"test");
				}
			});
	    	jdbcTemplate.update(DROP_TABLE_SQL);
	    }
	    
	    @Test
	    public void testJtaTransactionTemplate(){
	    	String[] configLocations = new String[]{
	    			"classpath:chapter9/applicationContext-jta-derby.xml"
	    	};
	    	ctx = new ClassPathXmlApplicationContext(configLocations);
	    	final PlatformTransactionManager jtaTXManager = ctx.getBean(PlatformTransactionManager.class);
	    	final DataSource derbyDataSource1 = ctx.getBean("dataSource1", DataSource.class);
	    	final DataSource derbyDataSource2 = ctx.getBean("dataSource2", DataSource.class);
	    	final JdbcTemplate jdbcTemplate1 = new JdbcTemplate(derbyDataSource1);
	    	final JdbcTemplate jdbcTemplate2 = new JdbcTemplate(derbyDataSource2);
	    	TransactionTemplate transactionTemplate = new TransactionTemplate(jtaTXManager);
	    	transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
	    	jdbcTemplate1.update(CREATE_TABLE_SQL);
	    	int originalCount = jdbcTemplate1.queryForInt(COUNT_SQL);
	    	try {
				transactionTemplate.execute(new TransactionCallbackWithoutResult() {
					
					
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus arg0) {
						// TODO Auto-generated method stub
						jdbcTemplate1.update(INSERT_SQL, "test");
						//因为数据库2没有创建数据库表因此会回滚事务
						jdbcTemplate2.update(INSERT_SQL, "test");
					}
				});
			} catch (RuntimeException e) {
				// TODO: handle exception
				int count = jdbcTemplate1.queryForInt(COUNT_SQL);
				Assert.assertEquals(originalCount, count);
			}
	    	jdbcTemplate1.update(DROP_TABLE_SQL);
	    }
	    
	    
	    
	    
	   
	    @Test
	    public void testServiceTransaction(){
	    	String[] configLocations = new String[] {
	    		"classpath:chapter7/applicationContext-resources.xml",
	    		"classpath:chapter9/dao/applicationContext-jdbc.xml",
	    		"classpath:chapter9/service/applicationContext-service.xml"
	    	};
	    	ApplicationContext ctx2 = new ClassPathXmlApplicationContext(configLocations);
	    	
	    	DataSource dataSource2 = ctx2.getBean(DataSource.class);
	    	JdbcTemplate jdbcTemplate2 = new JdbcTemplate(dataSource2);
	    	jdbcTemplate2.update(CREATE_USER_TABLE_SQL);
	    	jdbcTemplate2.update(CREATE_ADDRESS_TABLE_SQL);
	    	
	    	IUserService userService =ctx2.getBean("proxyUserService", IUserService.class);
	    	IAddressService addressService = ctx2.getBean("proxyAddressService", IAddressService.class);
	    	UserModel user = createDefaultUserModel();
	    	userService.save(user);
	    	Assert.assertEquals(1, userService.countAll());
	    	Assert.assertEquals(1, addressService.countAll());
	    	jdbcTemplate2.update(DROP_USER_TABLE_SQL);
	    	jdbcTemplate2.update(DROP_ADDRESS_TABLE_SQL);
	    }
	    
	    private UserModel createDefaultUserModel(){
	    	UserModel user = new UserModel();
	    	user.setName("test1");
	    	AddressModel address = new AddressModel();
	    	address.setProvince("beijing");
	    	address.setCity("beijing");
	    	address.setStreet("haidian");
	    	user.setAddress(address);
	    	return user;
	    }
	    
	    
	    
}

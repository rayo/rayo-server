package com.rayo.server.ameche;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.dom4j.Element;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;

import com.rayo.core.CallDirection;
import com.voxeo.logging.Loggerf;

/**
 * <p>Resolves Ameche routing rules defined on a database. 
 * Routing rules will have into consideration both to/from fields. All instances matching the given offer will 
 * be dispatched.</p>
 * 
 * @author martin
 *
 */
public class JdbcAppInstanceResolver implements AppInstanceResolver {

	private static final Loggerf logger = Loggerf.getLogger(JdbcAppInstanceResolver.class);

	private String lookupSql;
	private JdbcOperations jdbc;
	
	private RowMapper<AppInstance> rowMapper = new RowMapper<AppInstance>() {
		@Override
		public AppInstance mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			String id = rs.getString("appInstanceId");
			String uri = rs.getString("url");
			Integer priority = rs.getInt("priority");
			Integer permissions = rs.getInt("permissions");
			try {
				logger.debug("Found app instance [id=%s url=%s]", id, uri);
				return new AppInstance(id, new URI(uri), priority, permissions);
			} catch (URISyntaxException ex) {
				throw new IllegalStateException("Bad uri in database: " + uri.toString(), ex);
			}
		} 
	};
	
	
    @Override
    public List<AppInstance> lookup(Element offer, CallDirection direction) {
    	String from = offer.attributeValue("from");
    	String to = offer.attributeValue("to");
    	
    	logger.debug("Finding a match for [from:%s,  to:%s, direction:%s]", from, to, direction);
    	String address = ((direction == CallDirection.OUT) ? from : to);
    	return jdbc.query(lookupSql, new Object[] { address }, rowMapper);
    }
    
    public void setJdbcTemplate(JdbcOperations jdbc) {
    	this.jdbc = jdbc;
    }
    
    public void setLookupSql(String sql) {
    	this.lookupSql = sql;
    }
}

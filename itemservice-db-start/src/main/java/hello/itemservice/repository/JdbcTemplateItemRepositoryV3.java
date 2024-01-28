package hello.itemservice.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import hello.itemservice.domain.Item;
import lombok.extern.slf4j.Slf4j;

/**
 *  SimpleJdbcInsert
 *  - INSET SQL 작성하지 않고 INSERT 할 수 있는 법
 *  - JDBC Template 사용시 관례상 SimpleJdbcInsert 를 많이 사용한다.
 */
@Slf4j
@Repository
public class JdbcTemplateItemRepositoryV3 implements ItemRepository {

	private final NamedParameterJdbcTemplate template;
	private final SimpleJdbcInsert jdbcInsert;

	public JdbcTemplateItemRepositoryV3(DataSource dataSource) {
		this.template = new NamedParameterJdbcTemplate(dataSource);
		this.jdbcInsert = new SimpleJdbcInsert(dataSource)
			.withTableName("item")
			.usingGeneratedKeyColumns("id");
			//  생략시 모든 컬럼 사용
			// .usingColumns("item_name", "price", "quantity");
	}

	@Override
	public Item save(Item item) {
		BeanPropertySqlParameterSource param = new BeanPropertySqlParameterSource(item);
		Number key = jdbcInsert.executeAndReturnKey(param);
		item.setId(key.longValue());
		return item;
	}

	@Override
	public void update(Long itemId, ItemUpdateDto updateParam) {
		String sql = "update item set item_name=:itemName, price=:price, quantity=:quantity WHERE id = :id";

		MapSqlParameterSource param = new MapSqlParameterSource()
			.addValue("itemName", updateParam.getItemName())
			.addValue("price", updateParam.getPrice())
			.addValue("quantity", updateParam.getQuantity())
			.addValue("id", itemId);

		template.update(sql, param);
	}

	@Override
	public Optional<Item> findById(Long id) {
		String sql = "SELECT id, item_name, price, quantity FROM item WHERE id = :id";

		try {
			Map<String, Object> param = Map.of("id", id);
			Item item = template.queryForObject(sql, param, itemRowMapper());
			return Optional.of(item);
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Override
	public List<Item> findAll(ItemSearchCond cond) {
		String itemName = cond.getItemName();
		Integer maxPrice = cond.getMaxPrice();

		String sql = "SELECT id, item_name as itemName, price, quantity FROM item";
		BeanPropertySqlParameterSource param = new BeanPropertySqlParameterSource(cond);

		if(StringUtils.hasText(itemName) || maxPrice != null) {
			sql += " WHERE";
		}

		boolean andFlag = false;
		if (StringUtils.hasText(itemName)) {
			sql += " item_name like concat('%',:itemName,'%')";
			andFlag = true;
		}
		if (maxPrice != null) {
			if (andFlag) {
				sql += " and";
			}
			sql += " price <= :maxPrice";
		}
		log.info("sql={}", sql);
		return template.query(sql, param, itemRowMapper());
	}

	private RowMapper<Item> itemRowMapper() {
		return BeanPropertyRowMapper.newInstance(Item.class);
	}
}

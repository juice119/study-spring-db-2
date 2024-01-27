package hello.itemservice.repository;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import hello.itemservice.domain.Item;
import lombok.extern.slf4j.Slf4j;

/**
 * Jdbc Template
 */
@Slf4j
@Repository
public class JdbcTemplateItemRepositoryV2 implements ItemRepository {

	private final NamedParameterJdbcTemplate template;

	public JdbcTemplateItemRepositoryV2(DataSource dataSource) {
		this.template = new NamedParameterJdbcTemplate(dataSource);
	}

	@Override
	public Item save(Item item) {
		String sql = "INSERT INTO item (item_name, price, quantity) values (:itemName, :price, :quantity)";
		BeanPropertySqlParameterSource param = new BeanPropertySqlParameterSource(item);
		GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
		template.update(sql, param, keyHolder);

		long key = keyHolder.getKey().longValue();
		item.setId(key);
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

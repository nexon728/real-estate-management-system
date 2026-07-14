USE real_estate_db;

-- CS241 Deliverable 2: Required Queries
-- The script assumes the schema in CS241_Deliverable_2_Schema_Data.sql
-- has already been executed.

-- (a) Houses built after 2023 that are available for rent
SELECT
    p.property_id,
    p.address,
    p.city,
    p.area,
    p.bedrooms,
    p.year_built,
    l.listing_id,
    l.listed_price AS monthly_rent,
    l.status
FROM property AS p
JOIN listing AS l
    ON p.property_id = l.property_id
WHERE p.property_type = 'house'
  AND p.year_built > 2023
  AND l.type = 'rent'
  AND l.status = 'active';

-- (b) Average selling price of properties sold in 2018
SELECT
    ROUND(AVG(t.final_amount), 2) AS average_selling_price_2018
FROM transaction_ AS t
JOIN listing AS l
    ON t.listing_id = l.listing_id
WHERE l.type = 'sale'
  AND YEAR(t.transaction_date) = 2018;

-- (c) Active rent listings on GS Road with at least 2 bedrooms and rent below Rs. 15,000
SELECT
    p.property_id,
    p.address,
    p.area,
    p.bedrooms,
    l.listing_id,
    l.listed_price AS monthly_rent
FROM property AS p
JOIN listing AS l
    ON p.property_id = l.property_id
WHERE p.area = 'GS Road'
  AND l.type = 'rent'
  AND l.status = 'active'
  AND p.bedrooms >= 2
  AND l.listed_price < 15000;

-- (d) Agent who sold the most properties in 2023; tie broken by highest total sale amount
SELECT
    a.agent_id,
    pe.first_name,
    pe.last_name,
    COUNT(*) AS properties_sold_2023,
    SUM(t.final_amount) AS total_sale_amount_2023
FROM agent AS a
JOIN person AS pe
    ON a.person_id = pe.person_id
JOIN listing AS l
    ON a.agent_id = l.agent_id
JOIN transaction_ AS t
    ON l.listing_id = t.listing_id
WHERE l.type = 'sale'
  AND YEAR(t.transaction_date) = 2023
GROUP BY a.agent_id, pe.first_name, pe.last_name
ORDER BY properties_sold_2023 DESC, total_sale_amount_2023 DESC
LIMIT 1;

-- (e) Average time a property was on the market for each agent in 2018
SELECT
    a.agent_id,
    pe.first_name,
    pe.last_name,
    ROUND(AVG(DATEDIFF(t.transaction_date, l.list_date)), 2) AS avg_days_on_market_2018
FROM agent AS a
JOIN person AS pe
    ON a.person_id = pe.person_id
JOIN listing AS l
    ON a.agent_id = l.agent_id
JOIN transaction_ AS t
    ON l.listing_id = t.listing_id
WHERE l.type = 'sale'
  AND YEAR(t.transaction_date) = 2018
GROUP BY a.agent_id, pe.first_name, pe.last_name
ORDER BY avg_days_on_market_2018 DESC, a.agent_id;

-- (f) details of the most expensive houses and the houses with the highest rent
SELECT
    'Most Expensive House' AS query_type,
    p.property_id,
    p.address,
    p.city,
    p.area,
    p.property_type,
    p.size_sqft,
    p.bedrooms,
    p.year_built,
    l.listing_id,
    l.type AS listing_type,
    l.listed_price,
    l.status
FROM property AS p
JOIN listing AS l
    ON p.property_id = l.property_id
WHERE p.property_type = 'house'
  AND l.type = 'sale'
  AND l.listed_price = (
      SELECT MAX(l2.listed_price)
      FROM property AS p2
      JOIN listing AS l2
          ON p2.property_id = l2.property_id
      WHERE p2.property_type = 'house'
        AND l2.type = 'sale'
  )
UNION ALL
SELECT
    'House With Highest Rent' AS query_type,
    p.property_id,
    p.address,
    p.city,
    p.area,
    p.property_type,
    p.size_sqft,
    p.bedrooms,
    p.year_built,
    l.listing_id,
    l.type AS listing_type,
    l.listed_price,
    l.status
FROM property AS p
JOIN listing AS l
    ON p.property_id = l.property_id
WHERE p.property_type = 'house'
  AND l.type = 'rent'
  AND l.listed_price = (
      SELECT MAX(l2.listed_price)
      FROM property AS p2
      JOIN listing AS l2
          ON p2.property_id = l2.property_id
      WHERE p2.property_type = 'house'
        AND l2.type = 'rent'
  );
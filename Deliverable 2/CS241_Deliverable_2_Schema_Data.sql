-- CS241 Deliverable 2: Relational Design and Data Population
-- Database: MySQL
DROP DATABASE IF EXISTS real_estate_db;
CREATE DATABASE real_estate_db;
USE real_estate_db;

CREATE TABLE person (
    person_id INT PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE
) ;
CREATE TABLE person_phone (
    person_id INT NOT NULL,
    phone_number VARCHAR(15) NOT NULL,
    PRIMARY KEY (person_id, phone_number),
    CONSTRAINT fk_person_phone_person FOREIGN KEY (person_id) REFERENCES person(person_id) ON DELETE CASCADE ON UPDATE CASCADE
) ;
CREATE TABLE agent (
    agent_id INT PRIMARY KEY,
    person_id INT NOT NULL UNIQUE,
    commission_percent DECIMAL(5,2) NOT NULL,
    experience_years INT NOT NULL,
    CONSTRAINT fk_agent_person FOREIGN KEY (person_id) REFERENCES person(person_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_agent_commission CHECK (commission_percent >= 0 AND commission_percent <= 100),
    CONSTRAINT chk_agent_experience CHECK (experience_years >= 0)
) ;
CREATE TABLE property (
    property_id INT PRIMARY KEY,
    address VARCHAR(150) NOT NULL,
    city VARCHAR(60) NOT NULL,
    area VARCHAR(60) NOT NULL,
    property_type VARCHAR(20) NOT NULL,
    size_sqft INT NOT NULL,
    bedrooms INT NOT NULL,
    year_built INT NOT NULL,
    CONSTRAINT chk_property_type CHECK (property_type IN ('house', 'apartment')),
    CONSTRAINT chk_bedrooms CHECK (bedrooms > 0),
    CONSTRAINT chk_size CHECK (size_sqft > 0),
    CONSTRAINT chk_year_built CHECK (year_built BETWEEN 1900 AND 2100)
) ;
CREATE TABLE ownership (
    ownership_id INT PRIMARY KEY auto_increment,
    property_id INT NOT NULL,
    owner_id INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE DEFAULT NULL,
    CONSTRAINT fk_ownership_property FOREIGN KEY (property_id) REFERENCES property(property_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_ownership_owner FOREIGN KEY (owner_id) REFERENCES person(person_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_ownership_dates CHECK (end_date IS NULL OR end_date >= start_date)
) ;
CREATE TABLE listing (
    listing_id INT PRIMARY KEY,
    property_id INT NOT NULL,
    agent_id INT NOT NULL,
    type VARCHAR(10) NOT NULL,
    listed_price DECIMAL(12,2) NOT NULL,
    list_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT fk_listing_property FOREIGN KEY (property_id) REFERENCES property(property_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_listing_agent FOREIGN KEY (agent_id) REFERENCES agent(agent_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_listing_type CHECK (type IN ('sale', 'rent')),
    CONSTRAINT chk_listing_status CHECK (status IN ('active', 'sold', 'rented', 'withdrawn')),
    CONSTRAINT chk_listing_price CHECK (listed_price > 0)
) ;
CREATE TABLE transaction_ (
    transaction_id INT PRIMARY KEY,
    listing_id INT NOT NULL UNIQUE,
    customer_id INT NOT NULL,
    transaction_date DATE NOT NULL,
    final_amount DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_transaction_listing FOREIGN KEY (listing_id) REFERENCES listing(listing_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_transaction_customer FOREIGN KEY (customer_id) REFERENCES person(person_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_transaction_amount CHECK (final_amount > 0)
) ;
CREATE TABLE rent_detail (
    rent_detail_id INT PRIMARY KEY,
    transaction_id INT NOT NULL UNIQUE,
    lease_start DATE NOT NULL,
    lease_end DATE NOT NULL,
    monthly_rent DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_rent_detail_transaction FOREIGN KEY (transaction_id) REFERENCES transaction_(transaction_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_lease_dates CHECK (lease_end > lease_start),
    CONSTRAINT chk_monthly_rent CHECK (monthly_rent > 0)
) ;

CREATE INDEX idx_property_city_area ON property(city, area);
CREATE INDEX idx_property_type ON property(property_type);
CREATE INDEX idx_ownership_property_dates ON ownership(property_id, start_date, end_date);
CREATE INDEX idx_listing_agent_status ON listing(agent_id, status);
CREATE INDEX idx_listing_type_date ON listing(type, list_date);
CREATE INDEX idx_transaction_date ON transaction_(transaction_date);
CREATE INDEX idx_rent_detail_transaction ON rent_detail(transaction_id);

DELIMITER $$
CREATE TRIGGER trg_validate_transaction_date
BEFORE INSERT ON transaction_
FOR EACH ROW
BEGIN
    DECLARE listingDate DATE;
    SELECT list_date INTO listingDate FROM LISTING WHERE listing_id = NEW.listing_id;
    IF NEW.transaction_date < listingDate THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Transaction date cannot be before listing date';
    END IF;
END$$

CREATE TRIGGER trg_validate_rent_detail
BEFORE INSERT ON RENT_DETAIL
FOR EACH ROW
BEGIN
    DECLARE listingType VARCHAR(20);
    SELECT l.type INTO listingType FROM transaction_ t JOIN LISTING l ON t.listing_id = l.listing_id WHERE t.transaction_id = NEW.transaction_id;
    IF listingType = 'sale' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cannot add rent details for sale transaction';
    END IF;
END$$

CREATE TRIGGER trg_after_transaction_complete
AFTER INSERT ON transaction_
FOR EACH ROW
BEGIN
    DECLARE listingType VARCHAR(20);
    SELECT type INTO listingType FROM LISTING WHERE listing_id = NEW.listing_id;
    IF listingType = 'sale' THEN
        UPDATE LISTING SET status = 'sold' WHERE listing_id = NEW.listing_id;
    ELSEIF listingType = 'rent' THEN
        UPDATE LISTING SET status = 'rented' WHERE listing_id = NEW.listing_id;
    END IF;
END$$

CREATE TRIGGER trg_prevent_duplicate_active_listing
BEFORE INSERT ON LISTING
FOR EACH ROW
BEGIN
    DECLARE cnt INT;
    SELECT COUNT(*) INTO cnt FROM LISTING WHERE property_id = NEW.property_id AND status = 'active';
    IF cnt > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Property already has an active listing';
    END IF;
END$$

CREATE TRIGGER trg_validate_ownership_on_listing
BEFORE INSERT ON LISTING
FOR EACH ROW
BEGIN
    DECLARE cnt INT;
    SELECT COUNT(*) INTO cnt FROM OWNERSHIP WHERE property_id = NEW.property_id AND end_date IS NULL;
    IF cnt = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Property must have a valid owner before listing';
    END IF;
END$$

CREATE TRIGGER trg_update_ownership_after_sale
AFTER INSERT ON transaction_
FOR EACH ROW
BEGIN
    DECLARE listingType VARCHAR(20);
    DECLARE propId INT;
    SELECT l.type, l.property_id INTO listingType, propId FROM LISTING l WHERE l.listing_id = NEW.listing_id;
    IF listingType = 'sale' THEN
        UPDATE OWNERSHIP SET end_date = NEW.transaction_date WHERE property_id = propId AND end_date IS NULL AND NEW.transaction_date >= start_date;
        INSERT INTO OWNERSHIP(property_id, owner_id, start_date, end_date) VALUES (propId, NEW.customer_id, NEW.transaction_date, NULL);
    END IF;
END$$
DELIMITER ;

-- ==================== DATA EXPANSION ====================
-- Persons (1-30 Original + 31-50 New)
INSERT INTO person (person_id, first_name, last_name, email) VALUES
  (1, 'Aarav', 'Bora', 'aarav.bora1@example.com'),
  (2, 'Ananya', 'Ghosh', 'ananya.ghosh2@example.com'),
  (3, 'Rohan', 'Patel', 'rohan.patel3@example.com'),
  (4, 'Isha', 'Roy', 'isha.roy4@example.com'),
  (5, 'Vikram', 'Kumar', 'vikram.kumar5@example.com'),
  (6, 'Meera', 'Dutta', 'meera.dutta6@example.com'),
  (7, 'Arjun', 'Kapoor', 'arjun.kapoor7@example.com'),
  (8, 'Kavya', 'Mehta', 'kavya.mehta8@example.com'),
  (9, 'Nitin', 'Nath', 'nitin.nath9@example.com'),
  (10, 'Priya', 'Sharma', 'priya.sharma10@example.com'),
  (11, 'Sahil', 'Bora', 'sahil.bora11@example.com'),
  (12, 'Nora', 'Ghosh', 'nora.ghosh12@example.com'),
  (13, 'Rahul', 'Patel', 'rahul.patel13@example.com'),
  (14, 'Pooja', 'Roy', 'pooja.roy14@example.com'),
  (15, 'Kunal', 'Kumar', 'kunal.kumar15@example.com'),
  (16, 'Sneha', 'Dutta', 'sneha.dutta16@example.com'),
  (17, 'Aditya', 'Kapoor', 'aditya.kapoor17@example.com'),
  (18, 'Riya', 'Mehta', 'riya.mehta18@example.com'),
  (19, 'Manish', 'Nath', 'manish.nath19@example.com'),
  (20, 'Neha', 'Sharma', 'neha.sharma20@example.com'),
  (21, 'Deepak', 'Bora', 'deepak.bora21@example.com'),
  (22, 'Anita', 'Ghosh', 'anita.ghosh22@example.com'),
  (23, 'Suresh', 'Patel', 'suresh.patel23@example.com'),
  (24, 'Tanya', 'Roy', 'tanya.roy24@example.com'),
  (25, 'Harsh', 'Kumar', 'harsh.kumar25@example.com'),
  (26, 'Simran', 'Dutta', 'simran.dutta26@example.com'),
  (27, 'Karan', 'Kapoor', 'karan.kapoor27@example.com'),
  (28, 'Maya', 'Mehta', 'maya.mehta28@example.com'),
  (29, 'Gaurav', 'Nath', 'gaurav.nath29@example.com'),
  (30, 'Divya', 'Sharma', 'divya.sharma30@example.com'),
  (31, 'Amit', 'Singh', 'amit.singh31@example.com'),
  (32, 'Ravi', 'Verma', 'ravi.verma32@example.com'),
  (33, 'Neha', 'Gupta', 'neha.gupta33@example.com'),
  (34, 'Priya', 'Das', 'priya.das34@example.com'),
  (35, 'Raj', 'Malhotra', 'raj.malhotra35@example.com'),
  (36, 'Sneha', 'Reddy', 'sneha.reddy36@example.com'),
  (37, 'Karan', 'Joshi', 'karan.joshi37@example.com'),
  (38, 'Aarti', 'Chawla', 'aarti.chawla38@example.com'),
  (39, 'Vikas', 'Yadav', 'vikas.yadav39@example.com'),
  (40, 'Pooja', 'Mishra', 'pooja.mishra40@example.com'),
  (41, 'Sanjay', 'Pandey', 'sanjay.pandey41@example.com'),
  (42, 'Kavita', 'Saxena', 'kavita.saxena42@example.com'),
  (43, 'Rohit', 'Choudhary', 'rohit.choudhary43@example.com'),
  (44, 'Anjali', 'Tiwari', 'anjali.tiwari44@example.com'),
  (45, 'Rahul', 'Nair', 'rahul.nair45@example.com'),
  (46, 'Megha', 'Menon', 'megha.menon46@example.com'),
  (47, 'Tarun', 'Bansal', 'tarun.bansal47@example.com'),
  (48, 'Swati', 'Agarwal', 'swati.agarwal48@example.com'),
  (49, 'Nitin', 'Jain', 'nitin.jain49@example.com'),
  (50, 'Pallavi', 'Garg', 'pallavi.garg50@example.com');

INSERT INTO person_phone (person_id, phone_number) VALUES
  (1, '9700000001'), (2, '9700000002'), (3, '9700000003'), (4, '9700000004'),
  (5, '9700000005'), (6, '9700000006'), (7, '9700000007'), (8, '9700000008'),
  (9, '9700000009'), (10, '9700000010'), (11, '9700000011'), (12, '9700000012'),
  (13, '9700000013'), (14, '9700000014'), (15, '9700000015'), (16, '9700000016'),
  (17, '9700000017'), (18, '9700000018'), (1, '8600000001'), (4, '8600000004'),
  (7, '8600000007'), (10, '8600000010'), (13, '8600000013'), (18, '8600000018'),
  (19, '9700000019'), (20, '9700000020'), (21, '9700000021'), (22, '9700000022'),
  (23, '9700000023'), (24, '9700000024'), (25, '9700000025'), (26, '9700000026'),
  (27, '9700000027'), (28, '9700000028'), (29, '9700000029'), (30, '9700000030'),
  (31, '9700000031'), (32, '9700000032'), (33, '9700000033'), (34, '9700000034'),
  (35, '9700000035'), (36, '9700000036'), (37, '9700000037'), (38, '9700000038'),
  (39, '9700000039'), (40, '9700000040'), (41, '9700000041'), (42, '9700000042'),
  (43, '9700000043'), (44, '9700000044'), (45, '9700000045'), (46, '9700000046'),
  (47, '9700000047'), (48, '9700000048'), (49, '9700000049'), (50, '9700000050');

-- 1-12 Original, 13-20 New Agents
INSERT INTO agent (agent_id, person_id, commission_percent, experience_years) VALUES
  (1, 1, 2.0, 2), (2, 2, 2.5, 3), (3, 3, 3.0, 4), (4, 4, 3.5, 5),
  (5, 5, 1.5, 6), (6, 6, 2.0, 7), (7, 7, 2.5, 8), (8, 8, 3.0, 9),
  (9, 9, 3.5, 10), (10, 10, 1.5, 1), (11, 11, 2.0, 2), (12, 12, 2.5, 3),
  (13, 31, 2.0, 3), (14, 32, 2.5, 4), (15, 33, 3.0, 5), (16, 34, 1.5, 2),
  (17, 35, 2.0, 7), (18, 36, 3.5, 8), (19, 37, 2.5, 4), (20, 38, 3.0, 6);

-- Properties (1-24 Original + 25-45 New)
INSERT INTO property (property_id, address, city, area, property_type, size_sqft, bedrooms, year_built) VALUES
  (1, '12A, GS Road', 'Guwahati', 'GS Road', 'house', 1450, 2, 2024),
  (2, '14B, GS Road', 'Guwahati', 'GS Road', 'house', 1550, 3, 2025),
  (3, '8, Beltola', 'Guwahati', 'Beltola', 'apartment', 980, 2, 2022),
  (4, '21, Six Mile', 'Guwahati', 'Six Mile', 'house', 2100, 4, 2019),
  (5, '5, Zoo Road', 'Guwahati', 'Zoo Road', 'house', 1250, 3, 2018),
  (6, '44, GS Road', 'Guwahati', 'GS Road', 'house', 900, 2, 2021),
  (7, '10, Dispur', 'Guwahati', 'Dispur', 'apartment', 1100, 3, 2023),
  (8, '3, Khanapara', 'Guwahati', 'Khanapara', 'house', 1800, 3, 2020),
  (9, '17, Ulubari', 'Guwahati', 'Ulubari', 'apartment', 760, 1, 2024),
  (10, '29, Panjabari', 'Guwahati', 'Panjabari', 'house', 2400, 4, 2025),
  (11, '61, GS Road', 'Guwahati', 'GS Road', 'apartment', 1300, 3, 2017),
  (12, '18, Beltola', 'Guwahati', 'Beltola', 'house', 1700, 3, 2016),
  (13, '7, Dispur', 'Guwahati', 'Dispur', 'house', 1600, 3, 2024),
  (14, '2, Six Mile', 'Guwahati', 'Six Mile', 'apartment', 950, 2, 2025),
  (15, '50, Zoo Road', 'Guwahati', 'Zoo Road', 'house', 1900, 4, 2019),
  (16, '6, Khanapara', 'Guwahati', 'Khanapara', 'house', 1400, 2, 2023),
  (17, '22, Ulubari', 'Guwahati', 'Ulubari', 'house', 1450, 2, 2024),
  (18, '9, Panjabari', 'Guwahati', 'Panjabari', 'apartment', 1000, 2, 2021),
  (19, '11, GS Road', 'Guwahati', 'GS Road', 'house', 1350, 2, 2022),
  (20, '13, Beltola', 'Guwahati', 'Beltola', 'apartment', 840, 1, 2020),
  (21, '30, Ulubari', 'Guwahati', 'Ulubari', 'house', 1520, 3, 2023),
  (22, '4, Khanapara', 'Guwahati', 'Khanapara', 'apartment', 920, 2, 2018),
  (23, '72, Dispur', 'Guwahati', 'Dispur', 'house', 1750, 3, 2024),
  (24, '15, Zoo Road', 'Guwahati', 'Zoo Road', 'apartment', 880, 2, 2025),
  (25, '101, Bhangagarh', 'Guwahati', 'Bhangagarh', 'apartment', 1200, 2, 2015),
  (26, '202, Ganeshguri', 'Guwahati', 'Ganeshguri', 'house', 1800, 3, 2018),
  (27, '303, Maligaon', 'Guwahati', 'Maligaon', 'apartment', 900, 2, 2012),
  (28, '404, Paltan Bazaar', 'Guwahati', 'Paltan Bazaar', 'house', 2200, 4, 2020),
  (29, '505, Fancy Bazaar', 'Guwahati', 'Fancy Bazaar', 'apartment', 1500, 3, 2019),
  (30, '606, Silpukhuri', 'Guwahati', 'Silpukhuri', 'house', 1600, 3, 2016),
  (31, '707, Chandmari', 'Guwahati', 'Chandmari', 'apartment', 1100, 2, 2017),
  (32, '808, Hatigaon', 'Guwahati', 'Hatigaon', 'house', 1900, 4, 2021),
  (33, '909, Noonmati', 'Guwahati', 'Noonmati', 'apartment', 1000, 2, 2014),
  (34, '111, Jalukbari', 'Guwahati', 'Jalukbari', 'house', 2100, 4, 2022),
  (35, '222, Bhangagarh', 'Guwahati', 'Bhangagarh', 'house', 1700, 3, 2018),
  (36, '333, Ganeshguri', 'Guwahati', 'Ganeshguri', 'apartment', 1300, 3, 2019),
  (37, '444, Maligaon', 'Guwahati', 'Maligaon', 'house', 1400, 2, 2015),
  (38, '555, Paltan Bazaar', 'Guwahati', 'Paltan Bazaar', 'apartment', 950, 2, 2018),
  (39, '666, Fancy Bazaar', 'Guwahati', 'Fancy Bazaar', 'house', 2500, 5, 2023),
  (40, '777, Silpukhuri', 'Guwahati', 'Silpukhuri', 'apartment', 1050, 2, 2016),
  (41, '888, Chandmari', 'Guwahati', 'Chandmari', 'house', 1550, 3, 2020),
  (42, '999, Hatigaon', 'Guwahati', 'Hatigaon', 'apartment', 1250, 3, 2021),
  (43, '123, Noonmati', 'Guwahati', 'Noonmati', 'house', 1850, 4, 2017),
  (44, '456, Jalukbari', 'Guwahati', 'Jalukbari', 'apartment', 850, 1, 2014),
  (45, '789, GS Road', 'Guwahati', 'GS Road', 'house', 2000, 4, 2022);

-- Initial owners fixed for 1-24 to avoid buyer conflicts with persons 13-26. 
-- New property owners 25-45 assigned.
INSERT INTO ownership (ownership_id, property_id, owner_id, start_date, end_date) VALUES
  (1, 1, 31, '2017-01-01', NULL),
  (2, 2, 32, '2018-02-02', NULL),
  (3, 3, 33, '2019-03-03', NULL),
  (4, 4, 34, '2020-04-04', NULL),
  (5, 5, 35, '2021-05-05', NULL),
  (6, 6, 36, '2022-06-06', NULL),
  (7, 7, 37, '2023-07-07', NULL),
  (8, 8, 38, '2017-08-08', NULL),
  (9, 9, 39, '2018-09-09', NULL),
  (10, 10, 40, '2019-10-10', NULL),
  (11, 11, 41, '2020-11-11', NULL),
  (12, 12, 42, '2021-12-12', NULL),
  (13, 13, 43, '2022-01-13', NULL),
  (14, 14, 44, '2023-02-14', NULL),
  (15, 15, 45, '2017-03-15', NULL),
  (16, 16, 46, '2018-04-16', NULL),
  (17, 17, 47, '2019-05-17', NULL),
  (18, 18, 48, '2020-06-18', NULL),
  (19, 19, 49, '2021-07-19', NULL),
  (20, 20, 50, '2022-08-20', NULL),
  (21, 21, 31, '2023-09-01', NULL),
  (22, 22, 32, '2017-10-02', NULL),
  (23, 23, 33, '2018-11-03', NULL),
  (24, 24, 34, '2019-12-04', NULL),
  (25, 25, 39, '2016-01-10', NULL),
  (26, 26, 40, '2019-02-15', NULL),
  (27, 27, 41, '2013-03-20', NULL),
  (28, 28, 42, '2021-04-25', NULL),
  (29, 29, 43, '2020-05-30', NULL),
  (30, 30, 44, '2017-06-05', NULL),
  (31, 31, 45, '2018-07-10', NULL),
  (32, 32, 46, '2022-08-15', NULL),
  (33, 33, 47, '2015-09-20', NULL),
  (34, 34, 48, '2023-10-25', NULL),
  (35, 35, 49, '2019-11-30', NULL),
  (36, 36, 50, '2020-12-05', NULL),
  (37, 37, 39, '2016-01-15', NULL),
  (38, 38, 40, '2019-02-20', NULL),
  (39, 39, 41, '2023-03-25', NULL),
  (40, 40, 42, '2017-04-30', NULL),
  (41, 41, 43, '2021-05-05', NULL),
  (42, 42, 44, '2022-06-10', NULL),
  (43, 43, 45, '2018-07-15', NULL),
  (44, 44, 46, '2015-08-20', NULL),
  (45, 45, 47, '2023-09-25', NULL);

-- ====== PHASE 1: Listings 1-24 (all 'active' initially) ======
-- The transaction inserts below will cause the 'trg_after_transaction_complete'
-- trigger to automatically convert them to 'sold'/'rented' accurately.
INSERT INTO listing (listing_id, property_id, agent_id, type, listed_price, list_date, status) VALUES
  (1, 1, 1, 'sale', 3800000, '2017-02-15', 'active'),
  (2, 2, 2, 'sale', 5200000, '2017-04-01', 'active'),
  (3, 3, 3, 'sale', 2600000, '2017-05-16', 'active'),
  (4, 4, 4, 'sale', 6900000, '2017-06-30', 'active'),
  (5, 5, 5, 'sale', 4500000, '2017-08-14', 'active'),
  (6, 6, 6, 'rent', 14000, '2017-06-30', 'active'),
  (7, 7, 7, 'sale', 5800000, '2017-11-12', 'active'),
  (8, 8, 8, 'sale', 3400000, '2017-12-27', 'active'),
  (9, 9, 9, 'rent', 22000, '2017-09-28', 'active'),
  (10, 10, 10, 'sale', 7200000, '2018-03-27', 'active'),
  (11, 11, 11, 'rent', 12000, '2017-11-27', 'active'),
  (12, 12, 12, 'sale', 6100000, '2018-06-25', 'active'),
  (13, 13, 1, 'rent', 16000, '2018-01-26', 'active'),
  (14, 14, 2, 'rent', 18000, '2018-02-25', 'active'),
  (15, 15, 3, 'sale', 8800000, '2018-11-07', 'active'),
  (16, 16, 4, 'rent', 14500, '2018-04-26', 'active'),
  (17, 17, 5, 'rent', 12500, '2018-05-26', 'active'),
  (18, 18, 6, 'rent', 11000, '2018-06-25', 'active'),
  (19, 19, 7, 'rent', 14500, '2018-07-25', 'active'),
  (20, 20, 8, 'sale', 3000000, '2019-06-20', 'active'),
  (21, 21, 9, 'rent', 17000, '2018-09-23', 'active'),
  (22, 22, 10, 'rent', 13500, '2018-10-23', 'active'),
  (23, 23, 11, 'sale', 7600000, '2019-11-02', 'active'),
  (24, 24, 12, 'rent', 19000, '2018-12-22', 'active');

-- ====== PHASE 2: Transactions for listings 1-24 ======
-- Sale transactions trigger 'trg_after_transaction_complete' to set status='sold'/'rented'.
-- This clears property 1's active listing so it can be re-listed in Phase 3.
INSERT INTO transaction_ (transaction_id, listing_id, customer_id, transaction_date, final_amount) VALUES
  (1, 1, 13, '2018-03-15', 3850000),
  (2, 2, 14, '2018-07-21', 5350000),
  (15, 3, 21, '2018-09-12', 2550000),
  (3, 4, 15, '2018-11-05', 7050000),
  (16, 8, 22, '2018-12-18', 3380000),
  (4, 5, 16, '2023-02-10', 4480000),
  (5, 7, 17, '2023-08-19', 3420000),
  (6, 10, 18, '2023-12-01', 8950000),
  (7, 12, 19, '2023-09-30', 7700000),
  (8, 15, 20, '2023-04-17', 7650000),
  (9, 6, 21, '2024-01-05', 14000),
  (10, 9, 22, '2024-02-15', 22000),
  (11, 11, 23, '2024-03-10', 12000),
  (12, 14, 24, '2024-04-20', 11000),
  (13, 16, 25, '2024-05-02', 14500),
  (14, 21, 26, '2024-06-18', 15000);

-- Rent Details for Phase 2 rent transactions
INSERT INTO rent_detail (rent_detail_id, transaction_id, lease_start, lease_end, monthly_rent) VALUES
  (9, 9, '2024-01-05', '2025-01-04', 14000),
  (10, 10, '2024-02-15', '2025-02-14', 22000),
  (11, 11, '2024-03-10', '2025-03-09', 12000),
  (12, 12, '2024-04-20', '2025-04-19', 11000),
  (13, 13, '2024-05-02', '2025-05-01', 14500),
  (14, 14, '2024-06-18', '2025-06-17', 15000);

-- ====== PHASE 3: Listings 25-45 ======
-- Property 1 is now 'sold' (listing 1 status changed by trigger), so re-listing is allowed.
INSERT INTO listing (listing_id, property_id, agent_id, type, listed_price, list_date, status) VALUES
  (25, 1, 10, 'sale', 4100000, '2023-05-12', 'active'),
  (26, 25, 13, 'sale', 4200000, '2023-01-15', 'active'),
  (27, 26, 14, 'sale', 6500000, '2023-02-20', 'active'),
  (28, 27, 15, 'sale', 3100000, '2023-03-25', 'active'),
  (29, 28, 16, 'sale', 8500000, '2023-04-30', 'active'),
  (30, 29, 17, 'sale', 5400000, '2023-05-05', 'active'),
  (31, 30, 18, 'sale', 4900000, '2023-06-10', 'active'),
  (32, 31, 19, 'sale', 3800000, '2023-07-15', 'active'),
  (33, 32, 20, 'sale', 7200000, '2023-08-20', 'active'),
  (34, 33, 13, 'rent', 15000, '2023-09-25', 'active'),
  (35, 34, 14, 'rent', 25000, '2023-10-30', 'active'),
  (36, 35, 15, 'rent', 18000, '2023-11-05', 'active'),
  (37, 36, 16, 'rent', 16000, '2023-12-10', 'active'),
  (38, 37, 17, 'rent', 14000, '2024-01-15', 'active'),
  (39, 38, 18, 'rent', 12000, '2024-02-20', 'active'),
  (40, 39, 19, 'rent', 30000, '2024-03-25', 'active'),
  (41, 40, 20, 'sale', 3500000, '2024-04-30', 'active'),
  (42, 41, 13, 'sale', 5000000, '2024-05-05', 'active'),
  (43, 42, 14, 'rent', 17000, '2024-06-10', 'active'),
  (44, 43, 15, 'rent', 22000, '2024-07-15', 'active'),
  (45, 44, 16, 'sale', 2800000, '2024-08-20', 'active');

-- ====== PHASE 4: Transactions for listings 25-45 ======
INSERT INTO transaction_ (transaction_id, listing_id, customer_id, transaction_date, final_amount) VALUES
  (17, 25, 23, '2023-07-08', 4050000),
  (18, 26, 1, '2023-02-15', 4150000),
  (19, 27, 2, '2023-03-20', 6400000),
  (20, 28, 3, '2023-04-25', 3050000),
  (21, 29, 4, '2023-05-30', 8400000),
  (22, 30, 5, '2023-06-05', 5300000),
  (23, 31, 6, '2023-07-10', 4850000),
  (24, 32, 7, '2023-08-15', 3750000),
  (25, 33, 8, '2023-09-20', 7100000),
  (26, 34, 9, '2023-10-25', 15000),
  (27, 35, 10, '2023-11-30', 25000),
  (28, 36, 11, '2023-12-05', 18000),
  (29, 37, 12, '2024-01-10', 16000),
  (30, 38, 1, '2024-02-15', 14000),
  (31, 39, 2, '2024-03-20', 12000),
  (32, 40, 3, '2024-04-25', 30000);

-- Rent Details for Phase 4 rent transactions
INSERT INTO rent_detail (rent_detail_id, transaction_id, lease_start, lease_end, monthly_rent) VALUES
  (15, 26, '2023-11-01', '2024-10-31', 15000),
  (16, 27, '2023-12-01', '2024-11-30', 25000),
  (17, 28, '2024-01-01', '2024-12-31', 18000),
  (18, 29, '2024-02-01', '2025-01-31', 16000),
  (19, 30, '2024-03-01', '2025-02-28', 14000),
  (20, 31, '2024-04-01', '2025-03-31', 12000),
  (21, 32, '2024-05-01', '2025-04-30', 30000);

ALTER TABLE Orders ADD COLUMN activation_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE Participants ADD COLUMN timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE Tags ADD COLUMN icon VARCHAR(255);

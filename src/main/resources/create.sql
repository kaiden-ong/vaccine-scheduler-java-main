CREATE TABLE Caregivers (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Vaccines (
    Name varchar(255),
    Doses int,
    PRIMARY KEY (Name)
);

CREATE TABLE Patients (
	Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Availabilities (
    Time date,
    aptId int IDENTITY(0,1),
    cUsername varchar(255) REFERENCES Caregivers,
    pUsername varchar(255) REFERENCES Patients,
    vName varchar(255) REFERENCES Vaccines,
    PRIMARY KEY (aptId)
);
# ThriftyBackpackerProject131


## Creating a User Account

To create a new user, either:

1. **Via the frontend** — go to the Sign In page and register a new account (not implemented yet)
2. **Via SQL directly:**

```sql
docker exec -it thriftybackpacker-db psql -U postgres -d thriftybackpacker -c "INSERT INTO users (first_name, last_name, email, phone_number, password) VALUES ('Your', 'Name', 'your@email.com', '555-000-0000', 'CMPE-131@2026');"
```

Default password for all test accounts so far: `1234567`

> **Note:** Make sure Docker is running and the database container is up before running SQL commands.
> Start the database with: `cd BackEnd/java-backend && docker compose up -d`
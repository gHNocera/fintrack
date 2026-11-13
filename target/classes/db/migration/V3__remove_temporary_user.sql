DELETE FROM transactions
WHERE user_id IN (SELECT id FROM users WHERE email = 'temp@fintrack.local');

DELETE FROM categories
WHERE user_id IN (SELECT id FROM users WHERE email = 'temp@fintrack.local');

DELETE FROM users
WHERE email = 'temp@fintrack.local';
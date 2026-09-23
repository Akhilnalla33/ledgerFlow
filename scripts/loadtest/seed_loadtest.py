import json
import urllib.request
import sys

BASE = "http://localhost:8081"


def post(path, body):
    req = urllib.request.Request(
        BASE + path,
        data=json.dumps(body).encode(),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read())


def create_account(owner):
    return post("/api/v1/accounts", {"ownerId": owner, "currency": "USD"})["id"]


def mint(account_id, amount):
    req = urllib.request.Request(
        f"{BASE}/loadtest-only/mint?accountId={account_id}&amount={amount}",
        method="POST",
    )
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read())


N = int(sys.argv[1]) if len(sys.argv) > 1 else 50
accounts = []
for i in range(N):
    acc = create_account(f"loadtest-user-{i}")
    mint(acc, "1000000.00")
    accounts.append(acc)

with open("/tmp/loadtest_accounts.json", "w") as f:
    json.dump(accounts, f)

print(f"Seeded {len(accounts)} accounts, each with 1,000,000.00 USD")

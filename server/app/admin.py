import argparse
import getpass

from sqlalchemy import select

from app.auth.security import hash_password
from app.database import SessionLocal
from app.models import User


def read_password():
    password = getpass.getpass("New password: ")
    confirmation = getpass.getpass("Confirm password: ")

    if password != confirmation:
        raise SystemExit("Passwords do not match.")
    if len(password) < 12:
        raise SystemExit("Password must contain at least 12 characters.")

    return password


def create_user(username, is_admin):
    username = username.strip()
    if not username or len(username) > 50:
        raise SystemExit("Username must contain 1–50 characters.")

    password = read_password()

    with SessionLocal() as db:
        if db.scalar(select(User).where(User.username == username)):
            raise SystemExit("Username already exists.")

        user = User(
            username=username,
            password_hash=hash_password(password),
            is_active=True,
            is_admin=is_admin,
        )
        db.add(user)
        db.commit()
        print(f"User '{username}' created.")


def reset_password(username):
    password = read_password()

    with SessionLocal() as db:
        user = db.scalar(select(User).where(User.username == username))
        if user is None:
            raise SystemExit("User not found.")

        user.password_hash = hash_password(password)
        db.commit()
        print(f"Password reset for '{username}'.")


def list_users():
    with SessionLocal() as db:
        users = db.scalars(select(User).order_by(User.id)).all()
        for user in users:
            print(
                f"{user.id}: {user.username} | "
                f"{'admin' if user.is_admin else 'user'} | "
                f"{'active' if user.is_active else 'disabled'}"
            )


def main():
    parser = argparse.ArgumentParser(description="KinGz server account administration")
    commands = parser.add_subparsers(dest="command", required=True)

    create = commands.add_parser("create-user")
    create.add_argument("username")
    create.add_argument("--admin", action="store_true")

    reset = commands.add_parser("reset-password")
    reset.add_argument("username")

    commands.add_parser("list-users")

    args = parser.parse_args()

    if args.command == "create-user":
        create_user(args.username, args.admin)
    elif args.command == "reset-password":
        reset_password(args.username)
    elif args.command == "list-users":
        list_users()


if __name__ == "__main__":
    main()

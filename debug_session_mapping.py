from __future__ import annotations

import argparse
import hashlib
import json
import sqlite3
import sys
from pathlib import Path
from typing import Any, Iterable


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="调试微信会话表 Msg_xxx 与联系人/会话映射关系。"
    )
    parser.add_argument(
        "--db-root",
        required=True,
        help="db_storage 根目录，例如 C:\\Users\\xxx\\xwechat_files\\xxx\\db_storage",
    )
    parser.add_argument(
        "--message-db",
        default="message_0.db",
        help="消息库文件名，默认 message_0.db",
    )
    parser.add_argument(
        "--table",
        required=True,
        help="要调试的消息表名，可传 Msg_xxx 或 xxx",
    )
    parser.add_argument(
        "--sender-id",
        type=int,
        default=None,
        help="可选：日志里的 sender_id，用于辅助核对联系人映射",
    )
    parser.add_argument(
        "--keyword",
        default=None,
        help="可选：用显示名/备注/微信号做额外搜索",
    )
    parser.add_argument(
        "--limit",
        type=int,
        default=5,
        help="显示最近消息条数，默认 5",
    )
    parser.add_argument(
        "--config",
        default="config.yaml",
        help="配置文件路径，用于回退到 SDK 数据库访问模式时读取 dbkey，默认当前目录下 config.yaml",
    )
    return parser.parse_args()


def ro_connect(db_path: Path) -> sqlite3.Connection:
    return sqlite3.connect(f"file:{db_path.as_posix()}?mode=ro", uri=True)


def get_table_names(conn: sqlite3.Connection) -> list[str]:
    cur = conn.execute(
        "select name from sqlite_master where type='table' order by name"
    )
    return [row[0] for row in cur.fetchall()]


def get_columns(conn: sqlite3.Connection, table_name: str) -> list[str]:
    cur = conn.execute(f"PRAGMA table_info('{table_name}')")
    return [row[1] for row in cur.fetchall()]


def normalize(name: str) -> str:
    return name.replace("_", "").replace("-", "").lower()


def find_column(columns: Iterable[str], candidates: Iterable[str]) -> str | None:
    normalized = {normalize(col): col for col in columns}
    for candidate in candidates:
        if normalize(candidate) in normalized:
            return normalized[normalize(candidate)]
    return None


def row_to_dict(cursor: sqlite3.Cursor, row: tuple[Any, ...]) -> dict[str, Any]:
    return {description[0]: value for description, value in zip(cursor.description, row)}


def print_json(title: str, data: Any) -> None:
    print(f"\n=== {title} ===")
    print(json.dumps(data, ensure_ascii=False, indent=2, default=str))


def choose_candidate_tables(table_names: list[str], keyword: str) -> list[str]:
    lowered = keyword.lower()
    return [name for name in table_names if lowered in name.lower()]


def guess_contact_tables(table_names: list[str]) -> list[str]:
    candidates = choose_candidate_tables(table_names, "contact")
    return candidates or table_names


def guess_session_tables(table_names: list[str]) -> list[str]:
    candidates = choose_candidate_tables(table_names, "session")
    return candidates or table_names


def fetch_preview_rows(
    conn: sqlite3.Connection,
    table_name: str,
    limit: int,
    order_column: str | None = None,
) -> list[dict[str, Any]]:
    sql = f"select * from '{table_name}'"
    if order_column:
        sql += f" order by '{order_column}' desc"
    sql += f" limit {limit}"
    cur = conn.execute(sql)
    return [row_to_dict(cur, row) for row in cur.fetchall()]


def count_rows(conn: sqlite3.Connection, table_name: str) -> int | str:
    try:
        return conn.execute(f"select count(*) from '{table_name}'").fetchone()[0]
    except Exception as exc:
        return f"<count failed: {exc}>"


def build_contact_records(conn: sqlite3.Connection) -> list[dict[str, Any]]:
    records: list[dict[str, Any]] = []
    for table_name in guess_contact_tables(get_table_names(conn)):
        columns = get_columns(conn, table_name)
        username_col = find_column(columns, ["username", "user_name"])
        alias_col = find_column(columns, ["alias"])
        remark_col = find_column(columns, ["remark"])
        room_remark_col = find_column(columns, ["room_remark", "roomremark"])
        nickname_col = find_column(columns, ["nick_name", "nickname"])
        id_col = find_column(columns, ["id"])
        encrypt_username_col = find_column(columns, ["encrypt_username", "encryptusername"])

        if not username_col and not alias_col and not nickname_col:
            continue

        select_cols = ["rowid"]
        for col in [
            id_col,
            username_col,
            alias_col,
            remark_col,
            room_remark_col,
            nickname_col,
            encrypt_username_col,
        ]:
            if col and col not in select_cols:
                select_cols.append(col)

        cur = conn.execute(
            f"select {', '.join(f'\"{col}\"' for col in select_cols)} from '{table_name}'"
        )
        for row in cur.fetchall():
            row_dict = row_to_dict(cur, row)
            username = row_dict.get(username_col) if username_col else None
            alias = row_dict.get(alias_col) if alias_col else None
            remark = row_dict.get(remark_col) if remark_col else None
            room_remark = row_dict.get(room_remark_col) if room_remark_col else None
            nickname = row_dict.get(nickname_col) if nickname_col else None
            display_name = remark or room_remark or nickname or username or ""
            username_md5 = (
                hashlib.md5(str(username).encode("utf-8")).hexdigest() if username else None
            )
            records.append(
                {
                    "table": table_name,
                    "rowid": row_dict.get("rowid"),
                    "id": row_dict.get(id_col) if id_col else None,
                    "username": username,
                    "username_md5": username_md5,
                    "alias": alias,
                    "remark": remark,
                    "room_remark": room_remark,
                    "nickname": nickname,
                    "encrypt_username": (
                        row_dict.get(encrypt_username_col)
                        if encrypt_username_col
                        else None
                    ),
                    "display_name": display_name,
                }
            )
    return records


def build_session_records(conn: sqlite3.Connection) -> list[dict[str, Any]]:
    records: list[dict[str, Any]] = []
    for table_name in guess_session_tables(get_table_names(conn)):
        columns = get_columns(conn, table_name)
        username_col = find_column(columns, ["username", "user_name", "talker"])
        nickname_col = find_column(columns, ["nickname", "nick_name"])
        remark_col = find_column(columns, ["remark"])
        digest_col = find_column(columns, ["digest", "content"])
        unread_col = find_column(columns, ["unread_count", "unreadcount"])
        if not username_col:
            continue

        select_cols = ["rowid"]
        for col in [username_col, nickname_col, remark_col, digest_col, unread_col]:
            if col and col not in select_cols:
                select_cols.append(col)

        cur = conn.execute(
            f"select {', '.join(f'\"{col}\"' for col in select_cols)} from '{table_name}'"
        )
        for row in cur.fetchall():
            row_dict = row_to_dict(cur, row)
            username = row_dict.get(username_col)
            records.append(
                {
                    "table": table_name,
                    "rowid": row_dict.get("rowid"),
                    "username": username,
                    "username_md5": (
                        hashlib.md5(str(username).encode("utf-8")).hexdigest()
                        if username
                        else None
                    ),
                    "nickname": row_dict.get(nickname_col) if nickname_col else None,
                    "remark": row_dict.get(remark_col) if remark_col else None,
                    "digest": row_dict.get(digest_col) if digest_col else None,
                    "unread_count": row_dict.get(unread_col) if unread_col else None,
                }
            )
    return records


def search_contacts_by_keyword(records: list[dict[str, Any]], keyword: str) -> list[dict[str, Any]]:
    lowered = keyword.lower()
    result = []
    for record in records:
        haystacks = [
            record.get("display_name"),
            record.get("username"),
            record.get("alias"),
            record.get("remark"),
            record.get("nickname"),
            record.get("encrypt_username"),
        ]
        if any(value and lowered in str(value).lower() for value in haystacks):
            result.append(record)
    return result


def search_by_sender_id(records: list[dict[str, Any]], sender_id: int) -> list[dict[str, Any]]:
    return [record for record in records if record.get("id") == sender_id or record.get("rowid") == sender_id]


def trim_long_values(value: Any, max_len: int = 160) -> Any:
    if isinstance(value, bytes):
        return f"<bytes:{len(value)}>"
    if isinstance(value, str) and len(value) > max_len:
        return value[:max_len] + "...<trimmed>"
    return value


def sanitize_rows(rows: list[dict[str, Any]]) -> list[dict[str, Any]]:
    return [{key: trim_long_values(value) for key, value in row.items()} for row in rows]


def load_dbkey_from_config(config_path: Path) -> str | None:
    if not config_path.exists():
        return None
    try:
        for line in config_path.read_text(encoding="utf-8").splitlines():
            stripped = line.strip()
            if stripped.startswith("dbkey:"):
                value = stripped.split(":", 1)[1].split("#", 1)[0].strip()
                return value or None
    except Exception:
        return None
    return None


class SDKAccessor:
    def __init__(self, repo_root: Path, dbkey: str):
        src_dir = repo_root / "src"
        src_dir_str = str(src_dir)
        if src_dir_str not in sys.path:
            sys.path.insert(0, src_dir_str)

        from omni_bot_sdk.services.core.user_service import UserService
        from omni_bot_sdk.services.core.database_service import DatabaseService

        self.user_service = UserService(dbkey)
        self.db = DatabaseService(self.user_service)
        if hasattr(self.db, "setup"):
            self.db.setup()

    def close(self) -> None:
        if hasattr(self.db, "close"):
            try:
                self.db.close()
            except Exception:
                pass

    def execute_query(self, db_path: Path, query: str) -> list[tuple[Any, ...]]:
        return self.db.execute_query(db_path, query)

    def get_table_names(self, db_path: Path) -> list[str]:
        rows = self.execute_query(
            db_path,
            "select name from sqlite_master where type='table' order by name",
        )
        return [row[0] for row in rows]

    def get_columns(self, db_path: Path, table_name: str) -> list[str]:
        rows = self.execute_query(db_path, f"PRAGMA table_info('{table_name}')")
        return [row[1] for row in rows]

    def fetch_preview_rows(
        self,
        db_path: Path,
        table_name: str,
        limit: int,
        order_column: str | None = None,
    ) -> list[dict[str, Any]]:
        sql = f"select * from '{table_name}'"
        if order_column:
            sql += f" order by {order_column} desc"
        sql += f" limit {limit}"
        rows = self.execute_query(db_path, sql)
        columns = self.get_columns(db_path, table_name)
        return [dict(zip(columns, row)) for row in rows]

    def count_rows(self, db_path: Path, table_name: str) -> int | str:
        try:
            return self.execute_query(db_path, f"select count(*) from '{table_name}'")[0][0]
        except Exception as exc:
            return f"<count failed: {exc}>"

    def build_contact_records(self, db_path: Path) -> list[dict[str, Any]]:
        records: list[dict[str, Any]] = []
        for table_name in guess_contact_tables(self.get_table_names(db_path)):
            columns = self.get_columns(db_path, table_name)
            username_col = find_column(columns, ["username", "user_name"])
            alias_col = find_column(columns, ["alias"])
            remark_col = find_column(columns, ["remark"])
            room_remark_col = find_column(columns, ["room_remark", "roomremark"])
            nickname_col = find_column(columns, ["nick_name", "nickname"])
            id_col = find_column(columns, ["id"])
            encrypt_username_col = find_column(columns, ["encrypt_username", "encryptusername"])

            if not username_col and not alias_col and not nickname_col:
                continue

            select_cols: list[str] = []
            for col in [
                id_col,
                username_col,
                alias_col,
                remark_col,
                room_remark_col,
                nickname_col,
                encrypt_username_col,
            ]:
                if col and col not in select_cols:
                    select_cols.append(col)
            if not select_cols:
                continue

            rows = self.execute_query(
                db_path,
                f"select {', '.join(select_cols)} from '{table_name}'",
            )
            for row in rows:
                row_dict = dict(zip(select_cols, row))
                username = row_dict.get(username_col) if username_col else None
                alias = row_dict.get(alias_col) if alias_col else None
                remark = row_dict.get(remark_col) if remark_col else None
                room_remark = row_dict.get(room_remark_col) if room_remark_col else None
                nickname = row_dict.get(nickname_col) if nickname_col else None
                display_name = remark or room_remark or nickname or username or ""
                username_md5 = (
                    hashlib.md5(str(username).encode("utf-8")).hexdigest() if username else None
                )
                records.append(
                    {
                        "table": table_name,
                        "id": row_dict.get(id_col) if id_col else None,
                        "username": username,
                        "username_md5": username_md5,
                        "alias": alias,
                        "remark": remark,
                        "room_remark": room_remark,
                        "nickname": nickname,
                        "encrypt_username": (
                            row_dict.get(encrypt_username_col)
                            if encrypt_username_col
                            else None
                        ),
                        "display_name": display_name,
                    }
                )
        return records

    def build_session_records(self, db_path: Path) -> list[dict[str, Any]]:
        records: list[dict[str, Any]] = []
        for table_name in guess_session_tables(self.get_table_names(db_path)):
            columns = self.get_columns(db_path, table_name)
            username_col = find_column(columns, ["username", "user_name", "talker"])
            nickname_col = find_column(columns, ["nickname", "nick_name"])
            remark_col = find_column(columns, ["remark"])
            digest_col = find_column(columns, ["digest", "content"])
            unread_col = find_column(columns, ["unread_count", "unreadcount"])
            if not username_col:
                continue

            select_cols: list[str] = []
            for col in [username_col, nickname_col, remark_col, digest_col, unread_col]:
                if col and col not in select_cols:
                    select_cols.append(col)

            rows = self.execute_query(
                db_path,
                f"select {', '.join(select_cols)} from '{table_name}'",
            )
            for row in rows:
                row_dict = dict(zip(select_cols, row))
                username = row_dict.get(username_col)
                records.append(
                    {
                        "table": table_name,
                        "username": username,
                        "username_md5": (
                            hashlib.md5(str(username).encode("utf-8")).hexdigest()
                            if username
                            else None
                        ),
                        "nickname": row_dict.get(nickname_col) if nickname_col else None,
                        "remark": row_dict.get(remark_col) if remark_col else None,
                        "digest": row_dict.get(digest_col) if digest_col else None,
                        "unread_count": row_dict.get(unread_col) if unread_col else None,
                    }
                )
        return records


def main() -> int:
    args = parse_args()
    repo_root = Path(__file__).resolve().parent
    config_path = Path(args.config).expanduser().resolve()
    db_root = Path(args.db_root).expanduser().resolve()
    message_db = db_root / "message" / args.message_db
    contact_db = db_root / "contact" / "contact.db"
    session_db = db_root / "session" / "session.db"

    table_name = args.table if args.table.startswith("Msg_") else f"Msg_{args.table}"
    table_suffix = table_name.replace("Msg_", "", 1)

    print_json(
        "输入参数",
        {
            "db_root": str(db_root),
            "message_db": str(message_db),
            "contact_db": str(contact_db),
            "session_db": str(session_db),
            "table_name": table_name,
            "table_suffix": table_suffix,
            "sender_id": args.sender_id,
            "keyword": args.keyword,
            "limit": args.limit,
            "config_path": str(config_path),
        },
    )

    if not message_db.exists():
        print(f"消息库不存在: {message_db}")
        return 1
    if not contact_db.exists():
        print(f"联系人库不存在: {contact_db}")
        return 1
    if not session_db.exists():
        print(f"会话库不存在: {session_db}")
        return 1

    contact_records: list[dict[str, Any]]
    session_records: list[dict[str, Any]]
    try:
        with ro_connect(message_db) as msg_conn:
            msg_tables = get_table_names(msg_conn)
            if table_name not in msg_tables:
                print_json("消息库中的部分表名", msg_tables[:50])
                print(f"未找到消息表: {table_name}")
                return 1

            msg_columns = get_columns(msg_conn, table_name)
            order_column = find_column(
                msg_columns,
                ["create_time", "createtime", "sort_seq", "sortseq", "local_id", "localid"],
            )
            preview_rows = fetch_preview_rows(msg_conn, table_name, args.limit, order_column)
            print_json(
                "消息表信息",
                {
                    "mode": "sqlite3",
                    "table_name": table_name,
                    "row_count": count_rows(msg_conn, table_name),
                    "columns": msg_columns,
                    "order_column": order_column,
                    "latest_rows": sanitize_rows(preview_rows),
                },
            )

        with ro_connect(contact_db) as contact_conn:
            contact_records = build_contact_records(contact_conn)
        with ro_connect(session_db) as session_conn:
            session_records = build_session_records(session_conn)
    except sqlite3.DatabaseError as exc:
        print_json(
            "数据库打开失败，切换到 SDK 模式",
            {"error": str(exc), "hint": "数据库是加密库，改用 SDK 的 DatabaseService.execute_query"},
        )
        dbkey = load_dbkey_from_config(config_path)
        if not dbkey:
            print("未能从配置文件读取 dbkey，请确认 config.yaml 路径正确")
            return 1
        accessor = SDKAccessor(repo_root, dbkey)
        try:
            msg_tables = accessor.get_table_names(message_db)
            if table_name not in msg_tables:
                print_json("消息库中的部分表名", msg_tables[:50])
                print(f"未找到消息表: {table_name}")
                return 1
            msg_columns = accessor.get_columns(message_db, table_name)
            order_column = find_column(
                msg_columns,
                ["create_time", "createtime", "sort_seq", "sortseq", "local_id", "localid"],
            )
            preview_rows = accessor.fetch_preview_rows(
                message_db, table_name, args.limit, order_column
            )
            print_json(
                "消息表信息",
                {
                    "mode": "sdk",
                    "table_name": table_name,
                    "row_count": accessor.count_rows(message_db, table_name),
                    "columns": msg_columns,
                    "order_column": order_column,
                    "latest_rows": sanitize_rows(preview_rows),
                },
            )
            contact_records = accessor.build_contact_records(contact_db)
            session_records = accessor.build_session_records(session_db)
            if args.sender_id is not None:
                print_json(
                    "SDK 直接联系人映射",
                    {
                        "sender_id": args.sender_id,
                        "contact_by_sender_id": trim_long_values(
                            getattr(
                                accessor.db.get_contact_by_sender_id(
                                    args.sender_id, str(message_db)
                                ),
                                "__dict__",
                                None,
                            )
                        ),
                        "room_by_md5": trim_long_values(
                            getattr(
                                accessor.db.get_room_by_md5(table_suffix),
                                "__dict__",
                                None,
                            )
                        ),
                    },
                )
        finally:
            accessor.close()

    exact_contact_md5 = [
        record for record in contact_records if record.get("username_md5") == table_suffix
    ]
    exact_session_md5 = [
        record for record in session_records if record.get("username_md5") == table_suffix
    ]

    print_json("联系人表 Md5 精确匹配", exact_contact_md5[:20])
    print_json("会话表 Md5 精确匹配", exact_session_md5[:20])

    if args.sender_id is not None:
        print_json(
            "按 sender_id 匹配到的联系人候选",
            search_by_sender_id(contact_records, args.sender_id)[:20],
        )

    if args.keyword:
        print_json(
            "按关键词匹配到的联系人候选",
            search_contacts_by_keyword(contact_records, args.keyword)[:50],
        )

    summary = {
        "contact_exact_md5_count": len(exact_contact_md5),
        "session_exact_md5_count": len(exact_session_md5),
        "contact_record_count": len(contact_records),
        "session_record_count": len(session_records),
    }
    print_json("摘要", summary)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

# app/models.py
from datetime import datetime
from sqlalchemy import text
from sqlalchemy.dialects import mysql
from .extensions import db

# MySQL 실제 ENUM과 동일
CATEGORY_ENUM = ("POLITICS", "ECONOMY", "SOCIETY", "CULTURE", "INTERNATIONAL", "IT_SCIENCE")
DEDUP_ENUM    = ("REPRESENTATIVE", "RELATED", "KEPT")

class News(db.Model):
    __tablename__ = "news"
    __table_args__ = {"sqlite_autoincrement": True}

    news_id = db.Column(
        db.BigInteger().with_variant(db.Integer, "sqlite"),
        primary_key=True, autoincrement=True, nullable=False
    )

    # MySQL: varchar(255) UNIQUE NULL
    oid_aid = db.Column(db.String(255), unique=True, nullable=True)

    title = db.Column(db.Text, nullable=False)

    # MySQL ENUM
    category_name = db.Column(
        db.Enum(*CATEGORY_ENUM, name="category_name", native_enum=True),
        nullable=False
    )

    # MEDIUMTEXT on MySQL
    content = db.Column(
        db.Text().with_variant(mysql.MEDIUMTEXT(), "mysql"),
        nullable=False
    )

    press = db.Column(db.Text, nullable=False)

    # MySQL: datetime(6) NULL
    published_at = db.Column(
        db.DateTime().with_variant(mysql.DATETIME(fsp=6), "mysql"),
        nullable=True
    )

    reporter = db.Column(db.Text, nullable=False)

    # MySQL: datetime(6) NULL (기본값 없음)
    created_at = db.Column(
        db.DateTime().with_variant(mysql.DATETIME(fsp=6), "mysql"),
        nullable=True
    )
    updated_at = db.Column(
        db.DateTime().with_variant(mysql.DATETIME(fsp=6), "mysql"),
        nullable=True
    )

    dedup_state = db.Column(
        db.Enum(*DEDUP_ENUM, name="dedup_state", native_enum=True),
        nullable=False
    )

    image_url = db.Column(db.Text, nullable=True)

    # MySQL: tinyint(1) NULL
    trusted = db.Column(db.Boolean, nullable=True)


class NewsSummary(db.Model):
    __tablename__ = "news_summary"

    # MySQL: BIGINT AUTO_INCREMENT
    id = db.Column(
        db.BigInteger().with_variant(db.Integer, "sqlite"),
        primary_key=True, autoincrement=True
    )

    news_id = db.Column(
        db.BigInteger().with_variant(db.Integer, "sqlite"),
        db.ForeignKey("news.news_id"),
        index=True,
        nullable=True
    )

    # DB 컬럼명과 동일 (AIBOT / NEWSLETTER)
    summary_type = db.Column(db.String(50), nullable=False)

    # MEDIUMTEXT on MySQL
    summary_text = db.Column(
        db.Text().with_variant(mysql.MEDIUMTEXT(), "mysql"),
        nullable=False
    )

    # MySQL: datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
    created_at = db.Column(
        db.DateTime().with_variant(mysql.DATETIME(fsp=6), "mysql"),
        nullable=False,
        server_default=text("CURRENT_TIMESTAMP")
    )

    news = db.relationship("News", backref=db.backref("summaries", lazy=True))

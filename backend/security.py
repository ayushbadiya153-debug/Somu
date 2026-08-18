"""Symmetric encryption for storing Delta API secrets at rest."""
import base64
import hashlib
import os

from cryptography.hazmat.primitives.ciphers.aead import AESGCM

_ENC_KEY_ENV = os.environ["ENCRYPTION_KEY"]
# Derive stable 32-byte key from the env value.
_KEY = hashlib.sha256(_ENC_KEY_ENV.encode()).digest()
_aes = AESGCM(_KEY)


def encrypt_secret(plaintext: str) -> str:
    if not plaintext:
        return ""
    nonce = os.urandom(12)
    ct = _aes.encrypt(nonce, plaintext.encode(), None)
    return base64.b64encode(nonce + ct).decode()


def decrypt_secret(token: str) -> str:
    if not token:
        return ""
    raw = base64.b64decode(token.encode())
    nonce, ct = raw[:12], raw[12:]
    return _aes.decrypt(nonce, ct, None).decode()


def mask_secret(secret: str) -> str:
    if not secret:
        return ""
    if len(secret) <= 8:
        return "*" * len(secret)
    return secret[:4] + "*" * (len(secret) - 8) + secret[-4:]

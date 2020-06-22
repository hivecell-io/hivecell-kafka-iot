def require(val, error_msg="Error: Required field is empty"):
    if not val:
        raise ValueError(error_msg)
    return val


// NOTE: Return types are `any[]` here because the ag-grid column/row
// definitions are dynamically created from runtime data. Narrowing to the
// exact `ColDef` shape is possible but caused brittle type errors; using
// `any[]` is a pragmatic compromise for these helpers.
export function getColDefFromResult(data: unknown): any[] {
  if (!data) return [];
  const sample = Array.isArray(data) ? data[0] : data;
  if (!sample) return [];
  return Object.keys(sample).map((key) => {
    if (key === "active") {
      return {
        headerName: key.charAt(0).toUpperCase() + key.slice(1),
        field: key,
        cellRenderer: "agCheckboxCellRenderer",
        cellRendererParams: { tristate: false }, // display only, not for selection
        sortable: true,
        filter: false,
        width: 90,
      };
    }
    return {
      headerName: key.charAt(0).toUpperCase() + key.slice(1),
      field: key,
      sortable: true,
      filter: false,
    };
  });
}

export function getRowDataFromData(data: unknown): any[] {
  if (!data) return [];
  const arr = Array.isArray(data)
    ? data
    : typeof data === "object"
    ? [data]
    : [];
  return arr.map((row: Record<string, unknown>) => {
    let activeValue = row.active;
    if (typeof activeValue !== "boolean") {
      if (activeValue === "true" || activeValue === 1 || activeValue === "1") {
        activeValue = true;
      } else {
        activeValue = false;
      }
    }
    return {
      ...row,
      active: activeValue,
    };
  });
}

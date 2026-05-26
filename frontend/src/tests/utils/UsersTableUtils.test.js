import {
  toggleAdminMutation_params,
  toggleModeratorMutation_params,
} from "main/utils/UsersTableUtils";

describe("UsersTableUtils tests", () => {
  test("toggleAdminMutation_params returns correct axios params", () => {
    const cell = { row: { original: { id: 17 } } };
    const result = toggleAdminMutation_params(cell);
    expect(result).toEqual({
      url: "/api/admin/toggleAdmin",
      method: "PUT",
      params: { id: 17 },
    });
  });

  test("toggleModeratorMutation_params returns correct axios params", () => {
    const cell = { row: { original: { id: 42 } } };
    const result = toggleModeratorMutation_params(cell);
    expect(result).toEqual({
      url: "/api/admin/toggleModerator",
      method: "PUT",
      params: { id: 42 },
    });
  });
});

import {
  cellToAxiosParamsToggleAdmin,
  cellToAxiosParamsToggleModerator,
  onToggleSuccess,
} from "main/utils/Users";
import mockConsole from "tests/testutils/mockConsole";
import { vi } from "vitest";

const mockToast = vi.fn();
vi.mock("react-toastify", async () => {
  const originalModule = await vi.importActual("react-toastify");
  return {
    __esModule: true,
    ...originalModule,
    toast: (x) => mockToast(x),
  };
});

describe("Users Utils", () => {
  describe("cellToAxiosParamsToggleAdmin", () => {
    test("returns the correct params for toggle admin", () => {
      const cell = {
        row: {
          original: {
            id: 42,
          },
        },
      };

      const result = cellToAxiosParamsToggleAdmin(cell);

      expect(result).toEqual({
        url: "/api/admin/toggleAdmin",
        method: "PUT",
        params: {
          id: 42,
        },
      });
    });
  });

  describe("cellToAxiosParamsToggleModerator", () => {
    test("returns the correct params for toggle moderator", () => {
      const cell = {
        row: {
          original: {
            id: 42,
          },
        },
      };

      const result = cellToAxiosParamsToggleModerator(cell);

      expect(result).toEqual({
        url: "/api/admin/toggleModerator",
        method: "PUT",
        params: {
          id: 42,
        },
      });
    });
  });

  describe("onToggleSuccess", () => {
    test("It puts the message on console.log and in a toast", () => {
      const restoreConsole = mockConsole();

      onToggleSuccess("Admin status toggled");

      expect(console.log).toHaveBeenCalledWith("Admin status toggled");
      expect(mockToast).toHaveBeenCalledWith("Admin status toggled");

      restoreConsole();
    });

    test("uses the passed-in message for moderator toggle", () => {
      const restoreConsole = mockConsole();

      onToggleSuccess("Moderator status toggled");

      expect(console.log).toHaveBeenCalledWith("Moderator status toggled");
      expect(mockToast).toHaveBeenCalledWith("Moderator status toggled");

      restoreConsole();
    });
  });
});
